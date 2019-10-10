package org.act.temporal.test.tcypher;

import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import org.act.tgraph.demo.utils.TCypherClient;
import org.act.tgraph.demo.vo.RuntimeEnv;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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

        System.out.println("Host: "+ serverHost);
        System.out.println("Thread Num: "+threadCnt);
        System.out.println("Q/Tx: "+queryPerTx);
        System.out.println("Total line send: "+totalDataSize);
        System.out.println("Data path: "+ dataFilePath);
        try {
            TCypherClient client = new TCypherClient("cs-write-S-prop", serverHost, threadCnt, 2000, false);
            Map<String, Long> roadMap = client.start();
            System.out.println("id map built.");

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
            client.awaitTermination();
        } catch (IOException | ParseException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static SimpleDateFormat timeParser = new SimpleDateFormat("yyyyMMddHHmm");
    private static int parseTime(String yearMonthDay, String hourAndMinute) throws ParseException {
        return Math.toIntExact(timeParser.parse("20"+yearMonthDay+hourAndMinute).getTime()/1000);
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
