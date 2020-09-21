package simple.tgraph.kernel;

import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import edu.buaa.benchmark.BenchmarkTxResultProcessor;
import edu.buaa.benchmark.client.DBProxy;
import edu.buaa.benchmark.client.TGraphExecutorClient;
import edu.buaa.benchmark.transaction.EntityTemporalConditionTx;
import edu.buaa.utils.Helper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Condition;

public class EntityTemporalConditionTest {
    private static int threadCnt = Integer.parseInt(Helper.mustEnv("MAX_CONNECTION_CNT")); // number of threads to send queries.
    private static String serverHost = Helper.mustEnv("DB_HOST"); // hostname of TGraph (TCypher) server.
    private static boolean verifyResult = Boolean.parseBoolean(Helper.mustEnv("VERIFY_RESULT"));
    private static String resultFile = Helper.mustEnv("SERVER_RESULT_FILE");

    private static Producer logger;
    private static DBProxy client;
    private static BenchmarkTxResultProcessor post;

    @BeforeClass
    public static void init() throws IOException, ExecutionException, InterruptedException {
        client = new TGraphExecutorClient(serverHost, threadCnt, 800);
        client.testServerClientCompatibility();

        post = new BenchmarkTxResultProcessor("TGraph(EntityTemporalConditionTest)", Helper.codeGitVersion());
        logger = Helper.getLogger();
        post.setLogger(logger);
        post.setVerifyResult(verifyResult);
        post.setResult(new File(resultFile));
    }

    private static int ConditionValueMIN = 1200;
    private static int ConditionValueMAX = 600;

    @Test
    //travel_time < ???s
    public void EntityTemporalConditionInfo_vMIN() throws Exception{
        query_vMIN("travel_time", Helper.timeStr2int("201006300830"), Helper.timeStr2int("201006300930"), ConditionValueMIN);
    }

    @Test
    //travel_time > ???s
    public void EntityTemporalConditionInfo_vMAX() throws Exception{
        query_vMax("travel_time", Helper.timeStr2int("201006300830"), Helper.timeStr2int("201006300930"), ConditionValueMAX);
    }


    private void query_vMIN(String PropertyName, int st, int et, int vMIN) throws Exception{
        EntityTemporalConditionTx tx = new EntityTemporalConditionTx();
        tx.setP(PropertyName);
        tx.setT0(st);
        tx.setT1(et);
        tx.setVmin(vMIN);
        post.process(client.execute(tx), tx);
    }

    private void query_vMax(String PropertyName, int st, int et, int vMAX) throws Exception{
        EntityTemporalConditionTx tx = new EntityTemporalConditionTx();
        tx.setP(PropertyName);
        tx.setT0(st);
        tx.setT1(et);
        tx.setVmin(vMAX);
        post.process(client.execute(tx), tx);
    }

    @AfterClass
    public static void close() throws IOException, InterruptedException, ProducerException {
        post.close();
        client.close();
        logger.close();
    }

}
