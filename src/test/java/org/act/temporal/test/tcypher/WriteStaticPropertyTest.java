package org.act.temporal.test.tcypher;

import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import org.act.tgraph.demo.utils.TCypherClient;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *  create by sjh at 2019-09-10
 *
 *  Test TGraph Server TCypher 'property set' performance.
 */
public class WriteStaticPropertyTest {

    /**
     *
     * has 6 arguments:
     * 1. [dbPath] path of the TGraph database to get relationship id.
     * 2. [serverHost] hostname of TGraph (TCypher) server.
     * 3. [threadCount] number of threads to send queries.
     * 4. [queryPerTransaction] number of TCypher queries executed in one transaction.
     * 5. [totalDataSize] number of lines to read from data file.
     * 6. [dataFilePath] should be like '/media/song/test/data-set/beijing-traffic/TGraph/byday/100501'
     */
    public static void main(String[] args){
        if(args.length<6){
            System.out.println("need valid params.");
            return;
        }
        String dbPath = args[0];
        String serverHost = args[1];
        int threadCnt = Integer.parseInt(args[2]);
        int queryPerTx = Integer.parseInt(args[3]);
        int totalDataSize = Integer.parseInt(args[4]);
        String dataFilePath = args[5];

        System.out.println("DBPath: "+dbPath);
        System.out.println("Host: "+ serverHost);
        System.out.println("Thread Num: "+threadCnt);
        System.out.println("Q/Tx: "+queryPerTx);
        System.out.println("Total line send: "+totalDataSize);
        System.out.println("Data path: "+ dataFilePath);
        try {
            GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbPath));
            Map<String, Long> roadMap = buildRoadIDMap(db);
            db.shutdown();
            System.out.println("id map built.");

            TCypherClient client = new TCypherClient("cs-write-S-prop", serverHost, threadCnt, 2000, true);
            client.start();

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
                        if (dataInOneTx.size() == queryPerTx) {
                            client.addQuery(dataLines2tCypher( dataFileName, dataInOneTx, roadMap));
                            dataInOneTx.clear();
                        }
                    }else{
                        client.addQuery(dataLines2tCypher(dataFileName, dataInOneTx, roadMap));
                        dataInOneTx.clear();
                    }
                }
                while (lineSendCnt < totalDataSize && s!=null);
            }
            client.awaitSendDone();
        } catch (IOException | ParseException | InterruptedException | ProducerException e) {
            e.printStackTrace();
        }
    }

    private static SimpleDateFormat timeParser = new SimpleDateFormat("yyyyMMddHHmm");
    private static int parseTime(String yearMonthDay, String hourAndMinute) throws ParseException {
        return Math.toIntExact(timeParser.parse("20"+yearMonthDay+hourAndMinute).getTime()/1000);
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

    private static String dataLines2tCypher(String dataFileName, List<String> lines, Map<String, Long> roadMap) throws ParseException {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String[] arr = line.split(":");
            int time = parseTime(dataFileName, arr[0]);
            String[] d = arr[2].split(",");
            String q = "MATCH ()-[r:ROAD_TO]->() WHERE id(r)={0} SET " +
                    "r.travel_time_{1}={2}, " +
                    "r.full_status_{1}={3}, " +
                    "r.vehicle_count_{1}={4}, " +
                    "r.segment_count_{1}={5};";
            String qq = MessageFormat.format(q, String.valueOf(roadMap.get(arr[1])), String.format("%d",time), d[0], d[1], d[2], d[3]);
            sb.append(qq);
        }
        return sb.substring(0, sb.length()-1);
    }


}
