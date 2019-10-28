package run;

import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.log.common.LogItem;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.act.tgraph.demo.Config;
import org.act.tgraph.demo.utils.TGraphSocketClient;
import org.act.tgraph.demo.utils.TimeMonitor;
import org.act.tgraph.demo.vo.RuntimeEnv;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *  create by sjh at 2019-09-10
 *
 *  Test TGraph Server TCypher 'property set' performance.
 */

@RunWith(Parameterized.class)
public class TCypherWriteTemporalPropertyTest {
    private Producer logger = RuntimeEnv.getCurrentEnv().getConf().getLogger();
    private Config config = RuntimeEnv.getCurrentEnv().getConf();

    private int threadCnt; // number of threads to send queries.
    private int opPerTx; // number of TCypher queries executed in one transaction.
    private String serverHost; // hostname of TGraph (TCypher) server.
    private String dataFilePath; // should be like '/media/song/test/data-set/beijing-traffic/TGraph/byday/100501'
    private long totalDataSize; // number of lines to read from data file.

    public TCypherWriteTemporalPropertyTest(int threadCnt, int queryPerTx, String serverHost, String dataFilePath, long totalDataSize){
        this.threadCnt = threadCnt;
        this.opPerTx = queryPerTx;
        this.serverHost = serverHost;
        this.dataFilePath = dataFilePath;
        this.totalDataSize = totalDataSize;
    }

    protected String getTestName(){ return "cs-write-T-prop";}

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws ParseException {
        Config config = RuntimeEnv.getCurrentEnv().getConf();
        System.out.println("current runtime env: "+RuntimeEnv.getCurrentEnv().name());

        String serverHost = config.get("server_host").asString();
        int totalDataSize = 60_0000;
        String dataFileDir = config.get("dir_data_file_by_day").asString();

        return Arrays.asList(new Object[][] {
                { 20, 100, serverHost, getDataFilePath(dataFileDir, "2010.05.01"), totalDataSize }
        });
    }

    protected static String getDataFilePath(String dataFileDir, String day) throws ParseException {
        String fileName = new SimpleDateFormat("yyMMdd").format(new SimpleDateFormat("yyyy.MM.dd").parse(day));
        return new File(dataFileDir, fileName).getAbsolutePath();
    }

    public static void main(String[] args){
        if(args.length<6){
            System.out.println("need valid params.");
            return;
        }
        String serverHost = args[0];
        int threadCnt = Integer.parseInt(args[1]);
        int queryPerTx = Integer.parseInt(args[2]);
        int totalDataSize = Integer.parseInt(args[3]);
        String dataFilePath = args[4];

        System.out.println("Host: "+ serverHost);
        System.out.println("Thread Num: "+threadCnt);
        System.out.println("Q/Tx: "+queryPerTx);
        System.out.println("Total line send: "+totalDataSize);
        System.out.println("Data path: "+ dataFilePath);
        try {
            TCypherWriteTemporalPropertyTest test = new TCypherWriteTemporalPropertyTest(threadCnt, queryPerTx, serverHost, dataFilePath, totalDataSize);
            test.run();
        } catch (IOException | ParseException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void run() throws InterruptedException, ParseException, IOException, ExecutionException {
        runTest();
    }

    protected void runTest() throws InterruptedException, ParseException, IOException, ExecutionException {
        System.out.println("Host: " + serverHost);
        System.out.println("Thread Num: " + threadCnt);
        System.out.println("Q/Tx: " + opPerTx);
        System.out.println("Total line send: " + totalDataSize);
        System.out.println("Data path: " + dataFilePath);

        Client client = new Client( serverHost, threadCnt, 2000);
        client.testName = getTestName() +"-"+ new SimpleDateFormat ("yyyyMMdd_HHmm").format(new Date());
        client.logSource = RuntimeEnv.getCurrentEnv().name();

        String responseLine = client.addQuery("TOPIC:"+client.testName).get();
        if(responseLine.startsWith("Server code version:")) {
            String serverCodeVersion = responseLine.substring(20);
            String[] arr = serverCodeVersion.split("\\.");
            String gitVersion = config.codeGitVersion();
            if (Objects.equals(arr[1], gitVersion)){
                client.logSource += "->" + serverCodeVersion;
            }else{
                client.logSource += "."+gitVersion+"->" + serverCodeVersion;
            }
            System.out.println("logSource: "+ client.logSource);
        }else throw new RuntimeException("unexpected server response.");

        Future<String> f = client.addQuery("ID MAP");
        System.out.println("wait id map from server...");
        long t = System.currentTimeMillis();
        JsonObject obj = Json.parse(f.get()).asObject();
        System.out.println("done. wait " + (System.currentTimeMillis() - t) / 1000 + " seconds.");
        Map<String, Long> roadMap = new HashMap<>(140000);
        for (JsonObject.Member m : obj) {
            roadMap.put(m.getName(), m.getValue().asLong());
        }

        String dataFileName = new File(dataFilePath).getName(); // also is time by day. format yyMMdd
        long lineSendCnt = 0;
        try(BufferedReader br = new BufferedReader(new FileReader(dataFilePath)))
        {
            String s;
            List<String> dataInOneTx = new LinkedList<>();
            do{
                s = br.readLine();
                if(s!=null) {
                    lineSendCnt++;
                    dataInOneTx.add(s);
                    if (dataInOneTx.size() == opPerTx) {
                        client.addQuery(dataLines2Req( dataFileName, dataInOneTx, roadMap));
                        dataInOneTx.clear();
                    }
                    if(lineSendCnt %400==0) System.out.println(lineSendCnt +" line read.");
                }else{
                    client.addQuery(dataLines2Req(dataFileName, dataInOneTx, roadMap));
                    dataInOneTx.clear();
                }
            }
            while (lineSendCnt < totalDataSize && s!=null);
        }
        client.awaitTermination();
    }

    protected String dataLines2Req(String dataFileName, List<String> lines, Map<String, Long> roadMap) throws ParseException {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String[] arr = line.split(":");
            int time = parseTime(dataFileName, arr[0]);
            String[] d = arr[2].split(",");
            String q = "MATCH ()-[r:ROAD_TO]->() WHERE id(r)={0} SET " +
                    "r.travel_time=TV({1}~NOW:{2}), " +
                    "r.full_status=TV({1}~NOW:{3}), " +
                    "r.vehicle_count=TV({1}~NOW:{4}), " +
                    "r.segment_count=TV({1}~NOW:{5});";
            String qq = MessageFormat.format(q, String.valueOf(roadMap.get(arr[1])), String.valueOf(time), d[0], d[1], d[2], d[3]);
            sb.append(qq);
        }
        return sb.substring(0, sb.length()-1);
    }

    private SimpleDateFormat timeParser = new SimpleDateFormat("yyyyMMddHHmm");
    protected int parseTime(String yearMonthDay, String hourAndMinute) throws ParseException {
        return Math.toIntExact(timeParser.parse("20"+yearMonthDay+hourAndMinute).getTime()/1000);
    }

    private class Client extends TGraphSocketClient{
        String logSource;
        String testName;
        Client(String serverHost, int parallelCnt, int queueLength) throws IOException {
            super(serverHost, parallelCnt, queueLength);
        }

        @Override
        public String onResponse(String query, String response, TimeMonitor timeMonitor, Thread thread, Connection conn) throws Exception {
            JsonObject result = Json.parse(response).asObject();
            String resultContent = result.get("results").asString();
            if (resultContent.startsWith("Server code version:") || "ID MAP".equals(query)) return resultContent;

            LogItem log = new LogItem();
            log.PushBack("c_line_per_tx", String.valueOf(opPerTx));

            log.PushBack("type", "time");
            log.PushBack("c_thread", "T." + Thread.currentThread().getId());
            log.PushBack("c_queue_t", String.valueOf(timeMonitor.duration("Wait in queue")));
            log.PushBack("c_send_t", String.valueOf(timeMonitor.duration("Send query")));
            log.PushBack("c_send_tE", String.valueOf(timeMonitor.endT("Send query")));
            log.PushBack("c_wait_t", String.valueOf(timeMonitor.duration("Wait result")));

            log.PushBack("s_receive_tE", String.valueOf(result.get("t_ReqGot").asLong()));
            log.PushBack("s_tx_t", String.valueOf(result.get("t_Tx").asLong()));
            log.PushBack("s_psend_t", String.valueOf(result.get("t_PreSend").asLong()));
            log.PushBack("s_tx_success", String.valueOf(result.get("success").asBoolean()));
            log.PushBack("s_result_size", String.valueOf(resultContent.length()));


            log.PushBack("v_update_t", String.valueOf(result.get("s_updateTime").asLong()));
            log.PushBack("v_memory", String.valueOf(result.get("s_memory").asLong()));
            log.PushBack("v_connCnt", String.valueOf(result.get("s_connCnt").asLong()));
            log.PushBack("v_pCPU", String.valueOf(result.get("s_pCPU").asDouble()));
            log.PushBack("v_CPU", String.valueOf(result.get("s_CPU").asDouble()));
            log.PushBack("v_disk_qLen", String.valueOf(result.get("s_disk_qLen").asLong()));
            log.PushBack("v_disk_read", String.valueOf(result.get("s_disk_read").asLong()));
            log.PushBack("v_disk_write", String.valueOf(result.get("s_disk_write").asLong()));
            logger.send("tgraph-demo-test", "tgraph-log", testName, logSource, log);

//            // log result content;
//            LogItem resultLog = new LogItem();
//            resultLog.PushBack("type", "result");
//            resultLog.PushBack("c_send_tE", String.valueOf(timeMonitor.endT("Send query")));
//            resultLog.PushBack("c_thread", "T." + Thread.currentThread().getId());
//            resultLog.PushBack("s_result_content", resultContent);
//            logger.send("tgraph-demo-test", "tgraph-log", testName, logSource, resultLog);
            return resultContent;
        }
    }



//    @Test
//    public void tCypherTest(){
////        System.out.println(System.getProperty("java.vm.name"));
////        System.out.println(System.getProperty("java.vm.info"));
////        System.exit(0);
//        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File("/media/song/test/db-network-only-ro"));
//        Runtime.getRuntime().addShutdownHook(new Thread(db::shutdown));
//        long t = System.currentTimeMillis();
////        try (Transaction tx = db.beginTx()) {
////            db.getRelationshipById(1).setTemporalProperty("travel_time", 0, 2L);
////            tx.success();
////        }
//        try (Transaction tx = db.beginTx()) {
////            for(int i=0; i<10; i++) {
////                System.out.println(db.execute("MATCH ()-[r:ROAD_TO]->() WHERE id(r)=1 SET r.travel_time_100"+i+"="+(30+i)).resultAsString());
//                db.execute("MATCH ()-[r:ROAD_TO]->() WHERE id(r)=1 SET r.travel_time=TV(3~13:30, 100~NOW:2)");
////            }
//            tx.success();
//        }
//        System.out.println(System.currentTimeMillis() - t);
//        try (Transaction tx = db.beginTx()) {
//            for(int i=0; i<102; i++) {
//                System.out.println(db.getRelationshipById(1).getTemporalProperty("travel_time", i));
////                System.out.println(db.execute("MATCH ()-[r:ROAD_TO]->() WHERE r.id=1 SET r.travel_time_100="+(30+i)).resultAsString());
//            }
////            System.out.println(db.execute("MATCH ()-[r:ROAD_TO]->() WHERE r.id=1 SET r.travel_time=TV(100~NOW:30)").resultAsString());
//            tx.success();
//        }
//        System.out.println(System.currentTimeMillis() - t);
//        try(Transaction tx = db.beginTx()){
//            Relationship r = db.getRelationshipById(1);
////            for(String key : r.getPropertyKeys()){
////                System.out.println(key+": "+r.getProperty(key));
////            }
////            r.setTemporalProperty("travel_time", 400, 88);
//            r.getTemporalProperty("travel_time", 0, Integer.MAX_VALUE-4, new TemporalRangeQuery() {
//                @Override
//                public void setValueType(ValueContentType valueType) {
//                    System.out.println(valueType);
//                }
//
//                @Override
//                public void onNewEntry(InternalEntry entry) {
//                    System.out.print(entry.getKey().getStartTime()+":["+entry.getKey().getValueType()+"]"+entry.getValue().toString());
//                }
//
//                @Override
//                public Object onReturn() {
//                    return null;
//                }
//            });
//            tx.success();
//        }
//        System.out.println(System.currentTimeMillis() - t);
//    }
}
