package simple.tgraph.kernel.index;

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

public class EntityTemporalConditionIndexTest {
    private static int threadCnt = Integer.parseInt(Helper.mustEnv("MAX_CONNECTION_CNT")); // number of threads to send queries.
    private static String serverHost = Helper.mustEnv("DB_HOST"); // hostname of TGraph (TCypher) server.
    private static boolean verifyResult = Boolean.parseBoolean(Helper.mustEnv("VERIFY_RESULT"));
    private static String resultFile = Helper.mustEnv("SERVER_RESULT_FILE");
    private static String startDay = Helper.mustEnv("TEMPORAL_DATA_START"); //0501
    private static String endDay = Helper.mustEnv("TEMPORAL_DATA_END"); //0503
    private static int ConditionValue = Integer.parseInt(Helper.mustEnv("TEMPORAL_CONDITION"));

    private static Producer logger;
    private static DBProxy client;
    private static BenchmarkTxResultProcessor post;

    @BeforeClass
    public static void init() throws IOException, ExecutionException, InterruptedException {
        client = new TGraphExecutorClient(serverHost, threadCnt, 800);
        client.testServerClientCompatibility();

        post = new BenchmarkTxResultProcessor("TGraph(EntityTemporalConditionIndexTest)", Helper.codeGitVersion());
        logger = Helper.getLogger();
        post.setLogger(logger);
        post.setVerifyResult(verifyResult);
        post.setResult(new File(resultFile));
    }



    @Test
    //travel_time > ???s
    public void EntityTemporalConditionInfoVMIN() throws Exception{
        queryVMIN("travel_time", Helper.timeStr2int(startDay), Helper.timeStr2int(endDay), ConditionValue);
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
        post.close();
        client.close();
        logger.close();
    }

}
