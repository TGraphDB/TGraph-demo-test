package org.act.temporal.test.tcypher;

import org.act.tgraph.demo.utils.ProcessForkRunner;
import com.aliyun.openservices.aliyun.log.producer.LogProducer;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.ProducerConfig;
import com.aliyun.openservices.aliyun.log.producer.ProjectConfig;

import org.act.tgraph.demo.utils.TCypherTestServer;
import org.act.tgraph.demo.vo.Cross;
import org.act.tgraph.demo.vo.RelType;
import org.act.tgraph.demo.vo.RoadChain;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WriteTest {
    private static String dbPath = "/media/song/test/db-network-only";

    public static void main(String[] args){
        try {
            GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbPath));
            Map<String, Long> roadMap = buildRoadIDMap(db);
            db.shutdown();
//            System.out.println("id map built.");

            WriteTest t = new WriteTest();
            int waitingQueueSize = 1000;
            int maxThreadCnt = 4;
            t.startServer(maxThreadCnt, waitingQueueSize);

            Thread.sleep(20_000);
//            System.out.println("starting client.");

            int queryPerTx = 4;
            int totalDataSize = 20000;
            startClient(totalDataSize, queryPerTx, roadMap);

        } catch (IOException | ParseException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static SimpleDateFormat timeParser = new SimpleDateFormat("yyyyMMddHHmm");
    private static int parseTime(String s) throws ParseException {
        return Math.toIntExact(timeParser.parse("20"+s).getTime()/1000);
    }

    private static void startClient(int totalDataSize, int statementPerTx, Map<String, Long> roadMap) throws IOException, ParseException {
        Socket client = new Socket("127.0.0.1", 8438);
        client.setSoTimeout(8000);
        client.setTcpNoDelay(true);
        PrintWriter output = new PrintWriter(client.getOutputStream(), true);
        String dataPath = "/media/song/test/data-set/beijing-traffic/TGraph/temporal.data1456550604218.data";
        BufferedReader br = new BufferedReader(new FileReader(dataPath));
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

//        for(int i=0;i<358616; i++) in.readLine();

        long lineSendCnt = 0;
        try {
            while (lineSendCnt < totalDataSize) {
                String[] data = new String[statementPerTx];
                for (int i = 0; i < statementPerTx ; i++) {
                    String s = br.readLine();
                    if (s != null){
                        data[i] = s;
                        lineSendCnt++;
                    }
                }
                String query = dataLines2tCypher(data, roadMap);
                output.println(query);
                String response = in.readLine();
                while("SERVER BUSY".equals(response)){
                    Thread.sleep(10);
                    output.println(query);
                    response = in.readLine();
                }
            }
            output.println("EXIT");
        }catch (SocketException | InterruptedException ignored){
        }catch (SocketTimeoutException e){
            System.err.println("Server no response for 8 seconds, maybe error.");
        }
        System.out.println("Client exit. send "+ lineSendCnt+" lines.");
    }

    private void startServer(int maxThreadCnt, int waitingQueueSize) throws IOException {
        ProcessForkRunner mvnProcessBuilder = new ProcessForkRunner("/home/song/bin/apache-maven-3.3.1/bin/mvn", "/home/song/project/TGraph/source-code/public-test");
        mvnProcessBuilder.addArg("test").addArg("-Dtest=org.act.temporal.test.tcypher.WriteTest#runTestServer");
        mvnProcessBuilder.addArg("-DargLine='-Xms1g -Xmx12g'");
        mvnProcessBuilder.addEnv("tcnt", String.valueOf(maxThreadCnt));
        mvnProcessBuilder.addEnv("qsize", String.valueOf(waitingQueueSize));
        final Process process = mvnProcessBuilder.startProcess();
        new Thread(()->{
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while(true){
                try {
                    line = br.readLine();
                    if (line == null) break;
                    System.out.println(line);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * this runs in another process.
     * @throws IOException
     */
    @Test
    public void runTestServer() throws IOException {
//        System.getenv().forEach((k, v) -> {
//            System.out.println(k + ":" + v);
//        });
//        System.getProperties().forEach((k, v) -> {
//            System.out.println(k + ":" + v);
//        });
//        System.out.println(Runtime.getRuntime().maxMemory());
        int maxConcurrentCnt = Integer.parseInt(System.getenv("tcnt"));
        int waitingQueueSize = Integer.parseInt(System.getenv("qsize"));
        TCypherTestServer server = new TCypherTestServer(maxConcurrentCnt, waitingQueueSize, getLogger(), dbPath);
        server.start();
        System.out.println("Server started");
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

    private static Map<String, Long> buildRoadIDMap(GraphDatabaseService db) throws IOException {
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

    private static String dataLines2tCypher(String[] lines, Map<String, Long> roadMap) throws ParseException {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String[] arr = line.split(":");
            int time = parseTime(arr[0]);
            String[] d = arr[2].split(",");
            sb
                    .append("MATCH ()-[r:ROAD_TO]->() WHERE r.id=").append(roadMap.get(arr[1])).append(" SET ")
                    .append("r.travel_time=TV(").append(time).append("~NOW:").append(d[0]).append("),")
                    .append("r.full_status=TV(").append(time).append("~NOW:").append(d[1]).append("),")
                    .append("r.vehicle_count=TV(").append(time).append("~NOW:").append(d[2]).append("),")
                    .append("r.segment_count=TV(").append(time).append("~NOW:").append(d[3]).append(");");
        }
        return sb.toString();
    }

    private static Producer getLogger(){
        ProducerConfig pConf = new ProducerConfig();
        pConf.setIoThreadCount( 1 ); // one thread to upload
        Producer producer = new LogProducer( pConf );
        producer.putProjectConfig(new ProjectConfig("tgraph-demo-test", "cn-beijing.log.aliyuncs.com", "LTAIeA55PGyOpgZs", "H0XzCznABsioSI4TQpwXblH269eBm6"));
        return producer;
    }
}
