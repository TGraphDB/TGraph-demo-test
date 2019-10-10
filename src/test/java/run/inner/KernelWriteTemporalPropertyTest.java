package run.inner;


import org.act.tgraph.demo.Config;
import org.act.tgraph.demo.vo.RuntimeEnv;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import run.TCypherWriteTemporalPropertyTest;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;

@RunWith(Parameterized.class)
public class KernelWriteTemporalPropertyTest extends TCypherWriteTemporalPropertyTest {

    public KernelWriteTemporalPropertyTest(int threadCnt, int queryPerTx, String serverHost, String dataFilePath, long totalDataSize) {
        super(threadCnt, queryPerTx, serverHost, dataFilePath, totalDataSize);
    }

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

    @Override
    protected String dataLines2Req(String dataFileName, List<String> lines, Map<String, Long> roadMap) throws ParseException {
        List<String> req = new LinkedList<>();
        for (String line : lines) {
            String[] arr = line.split(":");
            int time = this.parseTime(dataFileName, arr[0]);
            String[] d = arr[2].split(",");
            String q = "{0},{1},{2},{3},{4},{5}";// 0.entity_id 1.time 2=d[0].travel_time 3=d[1].full_status 4=d[2].vehicle_count 5=d[3].segment_count
            String qq = MessageFormat.format(q, String.valueOf(roadMap.get(arr[1])), String.format("%d",time), d[0], d[1], d[2], d[3]);
            req.add(qq);
        }
        return String.join(";", req);
    }

}
