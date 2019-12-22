package run;

import org.act.tgraph.demo.client.Config;
import org.act.tgraph.demo.client.vo.RuntimeEnv;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 *  create by sjh at 2019-09-10
 *
 *  Test Neo4j Server Cypher 'property set' performance.
 *  same as TCypherWriteTemporalPropertyTest but use different cypher queries.
 */
@RunWith(Parameterized.class)
public class CypherWriteStaticPropertyTest extends TCypherWriteTemporalPropertyTest{
    @Override
    protected String getTestName(){return "cs-write-S-prop";}

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws ParseException {
        Config config = RuntimeEnv.getCurrentEnv().getConf();
        System.out.println("current runtime env: "+ RuntimeEnv.getCurrentEnv().name());

        String serverHost = config.get("server_host").asString();
        int totalDataSize = 60_0000;
        String dataFileDir = config.get("dir_data_file_by_day").asString();

        return Arrays.asList(new Object[][] {
                { 20, 100, serverHost, getDataFilePath(dataFileDir, "2010.05.01"), totalDataSize }
        });
    }

    public CypherWriteStaticPropertyTest(int threadCnt, int queryPerTx, String serverHost, String dataFilePath, long totalDataSize) {
        super(threadCnt, queryPerTx, serverHost, dataFilePath, totalDataSize);
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
            CypherWriteStaticPropertyTest test = new CypherWriteStaticPropertyTest(threadCnt, queryPerTx, serverHost, dataFilePath, totalDataSize);
            test.run();
        } catch (IOException | ParseException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String dataLines2Req(String dataFileName, List<String> lines, Map<String, Long> roadMap) throws ParseException {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String[] arr = line.split(":");
            int time = this.parseTime(dataFileName, arr[0]);
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
