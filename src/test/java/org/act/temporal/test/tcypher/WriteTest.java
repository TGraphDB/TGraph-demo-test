package org.act.temporal.test.tcypher;

import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.aliyun.openservices.log.common.LogItem;
import org.act.temporalProperty.impl.InternalEntry;
import org.act.temporalProperty.meta.ValueContentType;
import org.act.tgraph.demo.Config;
import org.act.tgraph.demo.utils.TimeMonitor;
import org.act.tgraph.demo.vo.Cross;
import org.act.tgraph.demo.vo.RelType;
import org.act.tgraph.demo.vo.RoadChain;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.temporal.TemporalRangeQuery;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.*;
import java.net.Socket;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class WriteTest {
    private static String testName = getTestName("server-write-S-prop");
    private static String host = "localhost";

    private static volatile boolean complete = false;

    public static void main(String[] args){
        if(args.length<7){
            System.out.println("need valid params.");
            return;
        }
        testName = getTestName(args[0]);
        String dbPath = args[1];
        host = args[2];
        int threadCnt = Integer.parseInt(args[3]);
        int queryPerTx = Integer.parseInt(args[4]);
        int totalDataSize = Integer.parseInt(args[5]);
        String dataFilePath = args[6];
        try {
            System.out.println("testName: "+testName+"\nDBPath: "+dbPath+"\nHost: "+host+"\nThread Num: "+threadCnt+
                    "\nQ/Tx: "+queryPerTx+"\nTotal line send: "+totalDataSize+"\nData path: "+ dataFilePath);
            GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbPath));
            Map<String, Long> roadMap = buildRoadIDMap(db);
            db.shutdown();
            System.out.println("id map built.");
            startClient(dataFilePath, totalDataSize, threadCnt, queryPerTx, roadMap);
        } catch (IOException | ParseException | InterruptedException | ProducerException e) {
            e.printStackTrace();
        }

    }

    private static SimpleDateFormat timeParser = new SimpleDateFormat("yyyyMMddHHmm");
    private static int parseTime(String s) throws ParseException {
        return Math.toIntExact(timeParser.parse("20"+s).getTime()/1000);
    }

    private static String getTestName(String name){
        SimpleDateFormat ft = new SimpleDateFormat ("yyyyMMdd_HHmm");
        return name + "-" + ft.format(new Date());
    }

    private static void startClient(String dataPath, int totalDataSize, int threadCnt, int statementPerTx, Map<String, Long> roadMap) throws IOException, ParseException, InterruptedException, ProducerException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(2000);
        List<Thread> threads = new LinkedList<>();
        for(int i=0; i<threadCnt; i++) {
            Thread t = new SendingThread(host, 8438, queue);
            threads.add(t);
            t.setDaemon(true);
            t.start();
        }
        queue.put("TOPIC:"+testName);

        long lineSendCnt = 0;
        try(BufferedReader br = new BufferedReader(new FileReader(dataPath)))
        {
            String s;
            List<String> dataInOneTx = new LinkedList<>();
            do{
                s = br.readLine();
                if(s!=null) {
                    lineSendCnt++;
                    dataInOneTx.add(s);
                    if (dataInOneTx.size() == statementPerTx) {
                        queue.put(dataLines2tCypher(dataInOneTx, roadMap));
                        dataInOneTx.clear();
                    }
                }else{
                    queue.put(dataLines2tCypher(dataInOneTx, roadMap));
                    dataInOneTx.clear();
                }
                if (lineSendCnt % 400 == 0)
                    System.out.println("reading " + lineSendCnt + " lines. queue size: " + queue.size());
            }
            while (lineSendCnt < totalDataSize && s!=null);
        }
        while(queue.size()>0) {
            Thread.sleep(10000);
            System.out.println("queue size: "+queue.size());
        }
        complete = true;
        for(Thread t : threads) t.join();
        Config.Default.onlineLogger.close();
        System.out.println("Client exit. send "+ lineSendCnt+" lines.");
    }

    private static class SendingThread extends Thread{
        Socket client;
        BufferedReader in;
        PrintWriter output;
        BlockingQueue<String> queue;
        SendingThread(String host, int port, BlockingQueue<String> queue) throws IOException {
            this.queue = queue;
            client = new Socket(host, port);
            client.setSoTimeout(8000);
            client.setTcpNoDelay(true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            output = new PrintWriter(client.getOutputStream(), true);
        }

        public void run(){
            try {
                Producer logger = Config.Default.onlineLogger;
                TimeMonitor timeMonitor = new TimeMonitor();
                timeMonitor.begin("Log");
                while(!complete){
                    timeMonitor.mark("Log", "Read query");
                    long previousLogT = timeMonitor.duration("Log");
                    String query;
                    try {
                        query = queue.poll(2, TimeUnit.SECONDS);
                        if(query==null || query.equals("GOT")) continue;
                    } catch (InterruptedException e) {
                        continue;
                    }
                    timeMonitor.mark("Read query", "Send query");
                    output.println(query);
                    timeMonitor.mark("Send query", "Wait result");
                    String[] result;
                    try {
                        String response = in.readLine();
                        timeMonitor.mark("Wait result", "Log");
                        result = response.split("AMITABHA");
                    } catch (IOException e) {
                        System.out.println("Server close connection.");
                        break;
                    }
                    LogItem log = new LogItem();
                    log.PushBack("c_read_t", String.valueOf(timeMonitor.duration("Read query")));
                    log.PushBack("c_send_t", String.valueOf(timeMonitor.duration("Send query")));
                    log.PushBack("c_send_tE", String.valueOf(timeMonitor.endT("Send query")));
                    log.PushBack("c_wait_t", String.valueOf(timeMonitor.duration("Wait result")));
                    log.PushBack("c_plog_t", String.valueOf(previousLogT));
                    log.PushBack("s_receive_tE", result[2]);
                    log.PushBack("s_tx_t", result[4]);
                    log.PushBack("s_psend_t", result[3]);
                    log.PushBack("s_tx_success", result[0]);
                    log.PushBack("s_result_size", result[1]);
                    log.PushBack("thread", Thread.currentThread().getName());
                    log.PushBack("tx_data_cnt", String.valueOf(result.length - 5));
                    try {
                        logger.send("tgraph-demo-test", "tgraph-log", testName, "sjh-ubuntu1804", log);
                    } catch (InterruptedException | ProducerException e) {
                        e.printStackTrace();
                    }
                }
                in.close();
                output.close();
                client.close();
                System.out.println("client connection close.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("client thread exit.");
        }
    }

    public void importNetwork2db() throws IOException {

        List<RoadChain> roadChainList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("/home/song/tmp/Topo.csv"))) {
            String line;
            for (int lineCount = 0; (line = br.readLine()) != null; lineCount++) {
                if (lineCount == 0) continue;//ignore headers
                try {
                    roadChainList.add(new RoadChain(line, lineCount));
                }catch (RuntimeException e){
                    System.out.println(e.getMessage()+" at line:"+lineCount);
                }
            }
        }
        for(RoadChain roadChain: roadChainList){
            roadChain.updateNeighbors();
        }

        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File("/tmp/test-db"));
        try(Transaction tx = db.beginTx()) {
            for (RoadChain roadChain : roadChainList) {
                int inCount = roadChain.getInNum();
                int outCount = roadChain.getOutNum();
                if (inCount > 0 || outCount > 0) {
                    Cross inCross = Cross.getStartCross(roadChain);
                    Cross outCross = Cross.getEndCross(roadChain);
                    Node inNode, outNode;
                    if (inCross.getNode(db) == null) {
                        inNode = db.createNode();
                        inCross.setNode(inNode);
                        inNode.setProperty("cross_id", inCross.id);
                    } else {
                        inNode = inCross.getNode(db);
                    }
                    if (outCross.getNode(db) == null) {
                        outNode = db.createNode();
                        outCross.setNode(outNode);
                        outNode.setProperty("cross_id", outCross.id);
                    } else {
                        outNode = outCross.getNode(db);
                    }

                    Relationship r = inNode.createRelationshipTo(outNode, RelType.ROAD_TO);
                    r.setProperty("grid_id", roadChain.getGridId());
                    r.setProperty("chain_id", roadChain.getChainId());
//                    r.setProperty("uid", roadChain.getUid());
//                    r.setProperty("type", roadChain.getType());
//                    r.setProperty("length", roadChain.getLength());
//                    r.setProperty("angle", roadChain.getAngle());
//                    r.setProperty("in_count", roadChain.getInNum());
//                    r.setProperty("out_count", roadChain.getOutNum());
//                    r.setProperty("in_roads", roadChain.briefInChain());
//                    r.setProperty("out_roads", roadChain.briefOutChain());
//                    r.setProperty("data_count", 0);
//                    r.setProperty("min_time", Integer.MAX_VALUE);
//                    r.setProperty("max_time", 0);
                }
            }
            tx.success();
        }
        db.shutdown();
    }

    private static Map<String, Long> buildRoadIDMap(GraphDatabaseService db) {
        Map<String, Long> map = new HashMap<>(130000);
        try(Transaction tx = db.beginTx()){
            for(Relationship r: GlobalGraphOperations.at(db).getAllRelationships()){
                String gridId = (String) r.getProperty("grid_id");
                String chainId = (String) r.getProperty("chain_id");
                String key = gridId+","+chainId;
                map.put(key, r.getId());
            }
            tx.success();
        }
        return map;
    }

    private static String dataLines2tCypher(List<String> lines, Map<String, Long> roadMap) throws ParseException {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String[] arr = line.split(":");
            int time = parseTime(arr[0]);
            String[] d = arr[2].split(",");
//            String q = "MATCH ()-[r:ROAD_TO]->() WHERE r.id={0} SET " +
//                    "r.travel_time=TV({1}~NOW:{2}), " +
//                    "r.full_status=TV({1}~NOW:{3}), " +
//                    "r.vehicle_count=TV({1}~NOW:{4}), " +
//                    "r.segment_count=TV({1}~NOW:{5});";
//            String qq = MessageFormat.format(q, roadMap.get(arr[1]), String.valueOf(time), d[0], d[1], d[2], d[3]);
            String q = "MATCH ()-[r:ROAD_TO]->() WHERE r.id={0} SET " +
                    "r.travel_time_{1}={2}, " +
                    "r.full_status_{1}={3}, " +
                    "r.vehicle_count_{1}={4}, " +
                    "r.segment_count_{1}={5};";
            String qq = MessageFormat.format(q, String.valueOf(roadMap.get(arr[1])), String.format("%d",time), d[0], d[1], d[2], d[3]);
            sb.append(qq);
        }
        return sb.substring(0, sb.length()-1);
    }

    @Test
    public void tCypherTest(){
//        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbPath));
//        Runtime.getRuntime().addShutdownHook(new Thread(db::shutdown));
//        try(Transaction tx = db.beginTx()){
////            System.out.println(db.execute("MATCH ()-[r:ROAD_TO]->() WHERE r.id=1 RETURN r.travel_time").resultAsString());
//            System.out.println(db.execute("MATCH ()-[r:ROAD_TO]->() WHERE r.grid_id=595640 AND r.chain_id=30003 SET r.travel_time=TV(100~NOW:30)").resultAsString());
//            tx.success();
//        }
//        try(Transaction tx = db.beginTx()){
//            Relationship r = db.getRelationshipById(1);
//            for(String key : r.getPropertyKeys()){
//                System.out.println(key+": "+r.getProperty(key));
//            }
//            r.getTemporalProperty("travel_time", 0, Integer.MAX_VALUE - 4, new TemporalRangeQuery() {
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
    }
}
