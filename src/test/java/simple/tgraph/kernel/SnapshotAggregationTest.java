package simple.tgraph.kernel;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import edu.buaa.benchmark.BenchmarkTxResultProcessor;
import edu.buaa.benchmark.client.DBProxy;
import edu.buaa.benchmark.client.TGraphExecutorClient;
import edu.buaa.benchmark.transaction.SnapshotQueryTx;
import edu.buaa.utils.Helper;
import org.apache.commons.lang.time.DateUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.cypher.internal.frontend.v2_3.ast.functions.Str;
import org.neo4j.unsafe.impl.batchimport.cache.idmapping.string.Radix;
import org.neo4j.unsafe.impl.batchimport.cache.idmapping.string.RadixCalculator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SnapshotAggregationTest {

    private static int threadCnt = Integer.parseInt(Helper.mustEnv("MAX_CONNECTION_CNT")); // number of threads to send queries.
    private static String serverHost = Helper.mustEnv("DB_HOST"); // hostname of TGraph (TCypher) server.
    private static boolean verifyResult = Boolean.parseBoolean(Helper.mustEnv("VERIFY_RESULT"));
    private static String resultFile = Helper.mustEnv("SERVER_RESULT_FILE");

    private static Producer logger;
    private static DBProxy client;
    private static BenchmarkTxResultProcessor post;

    private static String startTime =  "201006300830";
    private static String endTime = "201006300930";

    @BeforeClass
    public static void init() throws IOException, ExecutionException, InterruptedException {
        client = new TGraphExecutorClient(serverHost, threadCnt, 800);
        client.testServerClientCompatibility();

        post = new BenchmarkTxResultProcessor("TGraph(Snapshot)", Helper.codeGitVersion());
        logger = Helper.getLogger();
        post.setLogger(logger);
        post.setVerifyResult(verifyResult);
        post.setResult(new File(resultFile));
    }

    @Test
    //最慢同行时间
    public void travelTimeInfo() throws Exception {
        String tTI_startTime = startTime;
        List<Integer> lists = new ArrayList<>();
        while (Long.parseLong(tTI_startTime) <= Long.parseLong(endTime)){
            int travel_time = query("travel_time", Integer.parseInt(tTI_startTime));
            lists.add(travel_time);
            tTI_startTime = addTime(tTI_startTime,5);
        }
        int maxTravelTime = lists.stream().max(Integer::compareTo).get().intValue();
        System.out.println(maxTravelTime);
    }

    @Test
    //拥堵状况
    public void jam_statusInfo() throws Exception{
        String jsI_startTime = startTime;
        while(Long.parseLong(jsI_startTime) <= Long.parseLong(endTime)){
            query("jam_status",Integer.parseInt(jsI_startTime));
            jsI_startTime = addTime(jsI_startTime,5);
        }
    }

    @Test
    //时间-条件查询，emmm不太明白需要输出什么-。-
    public void travelTimeCompare() throws Exception {
        String tTC_startTime = startTime;
        while (Long.parseLong(tTC_startTime) <= Long.parseLong(endTime)) {
            int travel_time = query("travel_time", Integer.parseInt(tTC_startTime));
            if (travel_time > 600) {
                System.out.println(travel_time);
            }
            tTC_startTime = addTime(tTC_startTime, 5);
        }
    }

    private String addTime(String time,int offset){
        DateTime tempTime = DateUtil.parse(time, "yyyymmddHHMM");
        DateTime dateTime = DateUtil.offsetMinute(tempTime, offset);
        return Integer.valueOf(dateTime.getTime()/1000+"")+"";

    }
    private int query(String propertyName, int t) throws Exception {
        SnapshotQueryTx tx = new SnapshotQueryTx();
        tx.setPropertyName(propertyName);
        tx.setTimestamp(t);
        post.process(client.execute(tx), tx);
        return tx.getMetrics().getReqSize();
    }

    @AfterClass
    public static void close() throws IOException, InterruptedException, ProducerException {
        post.close();
        client.close();
        logger.close();
    }

}

