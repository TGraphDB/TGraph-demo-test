package org.act.temporal.test.tcypher;

import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import org.act.tgraph.demo.utils.TCypherClient;
import org.neo4j.graphdb.GraphDatabaseService;
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
import java.util.*;

/**
 *  create by sjh at 2019-09-10
 *
 *  Test TGraph Server TCypher 'property set' performance.
 */
public class WriteTemporalPropertyTest {

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
        if(args.length<7){
            System.out.println("need valid params.");
            return;
        }
        String dbPath = args[1];
        String serverHost = args[2];
        int threadCnt = Integer.parseInt(args[3]);
        int queryPerTx = Integer.parseInt(args[4]);
        int totalDataSize = Integer.parseInt(args[5]);
        String dataFilePath = args[6];

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
            TCypherClient client = new TCypherClient(getTestName("cs-write-T-prop"), serverHost, threadCnt, 2000);
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

    private static String getTestName(String name){
        SimpleDateFormat ft = new SimpleDateFormat ("yyyyMMdd_HHmmss");
        return name + "-" + ft.format(new Date());
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
            String q = "MATCH ()-[r:ROAD_TO]->() WHERE r.id={0} SET " +
                    "r.travel_time=TV({1}~NOW:{2}), " +
                    "r.full_status=TV({1}~NOW:{3}), " +
                    "r.vehicle_count=TV({1}~NOW:{4}), " +
                    "r.segment_count=TV({1}~NOW:{5});";
            String qq = MessageFormat.format(q, roadMap.get(arr[1]), String.valueOf(time), d[0], d[1], d[2], d[3]);
            sb.append(qq);
        }
        return sb.substring(0, sb.length()-1);
    }
}
