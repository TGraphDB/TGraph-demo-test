package simple.tgraph.kernel;

import com.alibaba.fastjson.parser.ParserConfig;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import edu.buaa.benchmark.BenchmarkTxResultProcessor;
import edu.buaa.benchmark.client.DBProxy;
import edu.buaa.benchmark.client.TGraphExecutorClient;
import edu.buaa.benchmark.transaction.EntityTemporalConditionTx;
import edu.buaa.utils.Helper;
import javafx.scene.shape.HLineTo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class EntityTemporalConditionTest {
    private static int threadCnt = Integer.parseInt(Helper.mustEnv("MAX_CONNECTION_CNT")); // number of threads to send queries.
    private static String serverHost = Helper.mustEnv("DB_HOST"); // hostname of TGraph (TCypher) server.
    private static boolean verifyResult = Boolean.parseBoolean(Helper.mustEnv("VERIFY_RESULT"));
    private static String resultFile = Helper.mustEnv("SERVER_RESULT_FILE");
    private static String dataFilePath = Helper.mustEnv("RESULT_DATA_PATH");
    private static String testPropertyName = Helper.mustEnv("TEST_PROPERTY_NAME");
    private static String startTime = Helper.mustEnv("TEMPORAL_DATA_START");
    private static String endTime = Helper.mustEnv("TEMPORAL_DATA_END");
    private static int conditionValue = Integer.parseInt(Helper.mustEnv("TEMPORAL_CONDITION"));
    private static String logTestName = Helper.mustEnv("LOG_TEST_NAME");

    private static Producer logger;
    private static DBProxy client;
    private static BenchmarkTxResultProcessor post;

    @BeforeClass
    public static void init() throws IOException, ExecutionException, InterruptedException {
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
        client = new TGraphExecutorClient(serverHost, threadCnt, 800);
        client.testServerClientCompatibility();

        post = new BenchmarkTxResultProcessor(logTestName, Helper.codeGitVersion());
        logger = Helper.getLogger();
        post.setLogger(logger);
        post.setVerifyResult(verifyResult);
        post.setResult(new File(dataFilePath,resultFile));
    }

    @Test
    //travel_time > ???s
    public void entityTemporalConditionInfo() throws Exception {
        for (int i = 0; i < 160; i++) {
            queryVMIN(testPropertyName, Helper.timeStr2int(startTime), Helper.timeStr2int(endTime), conditionValue);
            //queryVMIN("travel_time", Helper.timeStr2int("201006300830"), Helper.timeStr2int("201006300930"), 600);
        }
    }


    private void queryVMIN(String PropertyName, int st, int et, int vMIN) throws Exception{
            EntityTemporalConditionTx tx = new EntityTemporalConditionTx();
            tx.setP(PropertyName);
            tx.setT0(st);
            tx.setT1(et);
            tx.setVmin(vMIN);
            post.process(client.execute(tx), tx);
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
