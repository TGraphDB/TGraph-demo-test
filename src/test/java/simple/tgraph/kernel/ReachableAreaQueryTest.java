package simple.tgraph.kernel;

import com.alibaba.fastjson.parser.ParserConfig;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import edu.buaa.benchmark.BenchmarkTxResultProcessor;
import edu.buaa.benchmark.client.DBProxy;
import edu.buaa.benchmark.client.TGraphExecutorClient;
import edu.buaa.benchmark.transaction.ReachableAreaQueryTx;
import edu.buaa.utils.Helper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ReachableAreaQueryTest {
    private static int threadCnt = Integer.parseInt(Helper.mustEnv("MAX_CONNECTION_CNT")); // number of threads to send queries.
    private static String serverHost = Helper.mustEnv("DB_HOST"); // hostname of TGraph (TCypher) server.
    private static boolean verifyResult = Boolean.parseBoolean(Helper.mustEnv("VERIFY_RESULT"));
    private static String resultFile = Helper.mustEnv("SERVER_RESULT_FILE");
    private static String dataFilePath = Helper.mustEnv("RESULT_DATA_PATH");
    private static long testStartCrossId = Long.parseLong(Helper.mustEnv("TEST_START_CROSS_ID"));
    private static String startTime = Helper.mustEnv("TEMPORAL_DATA_START");
    private static String testTravelTime = Helper.mustEnv("TRAVEL_TIME");

    private static Producer logger;
    private static DBProxy client;
    private static BenchmarkTxResultProcessor post;

    @BeforeClass
    public static void init() throws IOException, ExecutionException, InterruptedException {
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
        client = new TGraphExecutorClient(serverHost, threadCnt, 800);
        client.testServerClientCompatibility();

        post = new BenchmarkTxResultProcessor("TGraph(ReachableAreaQueryTest)", Helper.codeGitVersion());
        logger = Helper.getLogger();
        post.setLogger(logger);
        post.setVerifyResult(verifyResult);
        post.setResult(new File(dataFilePath,resultFile));
    }

    @Test
    public void reachableAreaQueryInfo() throws Exception {
        query(testStartCrossId, Helper.timeStr2int(startTime), Helper.timeStr2int(testTravelTime));
        //query("travel_time", Helper.timeStr2int("201006300830"), Helper.timeStr2int("201006300930"));
    }

    private void query(long propertyName, int st, int tt) throws Exception {
        for(int i=0; i<160; i++) {
            ReachableAreaQueryTx tx = new ReachableAreaQueryTx();
            tx.setStartCrossId(propertyName);
            tx.setDepartureTime(st);
            tx.setTravelTime(tt);
            post.process(client.execute(tx), tx);
        }
    }

    @AfterClass
    public static void close() throws IOException, InterruptedException, ProducerException {
        client.close();
        while(true) {
            try {
                post.awaitDone(30, TimeUnit.SECONDS);
                break;
            } catch (InterruptedException e) {}
        }
        logger.close();
    }
}
