//package simple.tgraph.kernel.index;
//
//import com.aliyun.openservices.aliyun.log.producer.Producer;
//import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
//import edu.buaa.benchmark.BenchmarkTxResultProcessor;
//import edu.buaa.benchmark.client.DBProxy;
//import edu.buaa.benchmark.client.TGraphExecutorClient;
//import edu.buaa.benchmark.transaction.AbstractTransaction;
//import edu.buaa.benchmark.transaction.EntityTemporalConditionTx;
//import edu.buaa.benchmark.transaction.index.CreateTGraphAggrMaxIndexTx;
//import edu.buaa.benchmark.transaction.index.SnapshotAggrDurationIndexTx;
//import edu.buaa.utils.Helper;
//import org.junit.AfterClass;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Calendar;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//
//public class EntityTemporalConditionIndexTest {
//    private static int threadCnt = Integer.parseInt(Helper.mustEnv("MAX_CONNECTION_CNT")); // number of threads to send queries.
//    private static String serverHost = Helper.mustEnv("DB_HOST"); // hostname of TGraph (TCypher) server.
//    private static boolean verifyResult = Boolean.parseBoolean(Helper.mustEnv("VERIFY_RESULT"));
//    private static String resultFile = Helper.mustEnv("SERVER_RESULT_FILE");
//    private static String dataFilePath = Helper.mustEnv("RAW_DATA_PATH");
//    private static String testPropertyName = Helper.mustEnv("TEST_PROPERTY_NAME");
//    private static String startTime = Helper.mustEnv("TEMPORAL_DATA_START");
//    private static String endTime = Helper.mustEnv("TEMPORAL_DATA_END");
//    private static String indexStartTime = Helper.mustEnv("INDEX_TEMPORAL_DATA_START");
//    private static String indexEndTime = Helper.mustEnv("INDEX_TEMPORAL_DATA_END_INDEX");
//    private static int vMin = Integer.parseInt(Helper.mustEnv("TEMPORAL_CONDITION"));
//    private static int vMAX = Integer.parseInt(Helper.mustEnv("TEMPORAL_CONDITION"));
//    private static long indexId = Long.valueOf(Helper.mustEnv("INDEX_ID"));
//
//    private static Producer logger;
//    private static DBProxy client;
//    private static BenchmarkTxResultProcessor post;
//
//    @BeforeClass
//    public static void init() throws IOException, ExecutionException, InterruptedException {
//        client = new TGraphExecutorClient(serverHost, threadCnt, 800);
//        client.testServerClientCompatibility();
//
//        post = new BenchmarkTxResultProcessor("TGraph(EntityTemporalConditionIndexTest)", Helper.codeGitVersion());
//        logger = Helper.getLogger();
//        post.setLogger(logger);
//        post.setVerifyResult(verifyResult);
//        post.setResult(new File(resultFile));
//    }
//
//
//
//    @Test
//    //travel_time > ???s
//    public void EntityTemporalConditionInfoVMIN() throws Exception{
//        query("travel_time", Helper.timeStr2int(startTime), Helper.timeStr2int(endTime), vMin,vMAX,indexId);
//    }
//
//
//    private void query(String PropertyName, int st, int et, int vMIN, int vMAX, long indexID) throws Exception{
//        EntityTemporalConditionTx tx = new EntityTemporalConditionTx();
//        tx.setP(PropertyName);
//        tx.setT0(st);
//        tx.setT1(et);
//        tx.setVmin(vMIN);
//        tx.setVmax(vMAX);
//        post.process(client.execute(tx), tx);
//    }
//
//    @AfterClass
//    public static void close() throws IOException, InterruptedException, ProducerException {
//        client.close();
//        while(true) {
//            try {
//                post.awaitDone(30, TimeUnit.SECONDS);
//                break;
//            } catch (InterruptedException e) {}
//        }
//        logger.close();
//    }
//}
