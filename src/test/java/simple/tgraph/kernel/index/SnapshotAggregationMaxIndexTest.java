package simple.tgraph.kernel.index;

import com.alibaba.fastjson.parser.ParserConfig;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.google.common.util.concurrent.ListenableFuture;
import edu.buaa.benchmark.BenchmarkTxResultProcessor;
import edu.buaa.benchmark.client.DBProxy;
import edu.buaa.benchmark.client.TGraphExecutorClient;
import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.benchmark.transaction.index.CreateTGraphAggrMaxIndexTx;
import edu.buaa.benchmark.transaction.index.SnapshotAggrMaxIndexTx;
import edu.buaa.utils.Helper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.kernel.impl.util.register.NeoRegister;

import javax.management.Query;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SnapshotAggregationMaxIndexTest {

    private static int threadCnt = Integer.parseInt(Helper.mustEnv("MAX_CONNECTION_CNT")); // number of threads to send queries.
    private static String serverHost = Helper.mustEnv("DB_HOST"); // hostname of TGraph (TCypher) server.
    private static boolean verifyResult = Boolean.parseBoolean(Helper.mustEnv("VERIFY_RESULT"));
    private static String resultFile = Helper.mustEnv("SERVER_RESULT_FILE");
    private static String dataFilePath = Helper.mustEnv("RESULT_DATA_PATH");
    private static String testPropertyName = Helper.mustEnv("TEST_PROPERTY_NAME");
    private static String startTime = Helper.mustEnv("TEMPORAL_DATA_START");
    private static String endTime = Helper.mustEnv("TEMPORAL_DATA_END");
//    private static String indexStartTime = Helper.mustEnv("INDEX_TEMPORAL_DATA_START");
//    private static String indexEndTime = Helper.mustEnv("INDEX_TEMPORAL_DATA_END");
    private static String indexId = Helper.mustEnv("INDEX_ID_OF_MAX");
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
    public void snapshotAggregationMaxIndexTestInfo() throws Exception{
        for(int i=0;i<200;i++) {
            query(testPropertyName, Helper.timeStr2int(startTime), Helper.timeStr2int(endTime), Long.parseLong(indexId));
        }
    }

//    private long createIndex() throws Exception {
//        CreateTGraphAggrMaxIndexTx tx = new CreateTGraphAggrMaxIndexTx();
//        tx.setProName("travel_time");
//        tx.setStart(Helper.timeStr2int(indexStartTime));
//        tx.setEnd(Helper.timeStr2int(indexEndTime));
//        tx.setEvery(1);
//        tx.setTimeUnit(Calendar.HOUR);
//        DBProxy.ServerResponse response = post.processSync(client.execute(tx), tx);
//        AbstractTransaction.Result result = response.getResult();
//        CreateTGraphAggrMaxIndexTx.Result res = (CreateTGraphAggrMaxIndexTx.Result) result;
//        long indexId = res.getIndexId();
//        return indexId;
//    }
    private void query(String propertyName, int st, int et, long indexID) throws Exception{
        SnapshotAggrMaxIndexTx tx = new SnapshotAggrMaxIndexTx();
        tx.setP(propertyName);
        tx.setT0(st);
        tx.setT1(et);
        tx.setIndexId(indexID);
        post.process(client.execute(tx), tx);
    }

    @AfterClass
    public static void close() throws IOException, InterruptedException, ProducerException {
        client.close();
        while(true) {
            try {
                post.awaitDone(30, TimeUnit.SECONDS);
                break;
            } catch (InterruptedException ignored) {}
        }
        logger.close();
    }
}
