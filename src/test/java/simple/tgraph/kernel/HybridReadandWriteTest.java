package simple.tgraph.kernel;

import com.alibaba.fastjson.parser.ParserConfig;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.sun.jna.platform.win32.WinBase;
import edu.buaa.benchmark.BenchmarkTxGenerator;
import edu.buaa.benchmark.BenchmarkTxResultProcessor;
import edu.buaa.benchmark.client.DBProxy;
import edu.buaa.benchmark.client.TGraphExecutorClient;
import edu.buaa.benchmark.transaction.*;
import edu.buaa.utils.Helper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static sun.misc.PostVMInitHook.run;

public class HybridReadandWriteTest {
    private static int writeThreadCnt = Integer.parseInt(Helper.mustEnv("WRITE_CONNECTION_CNT")); // number of threads to send queries..
    private static int readThreadCnt = Integer.parseInt(Helper.mustEnv("READ_CONNECTION_CNT")); // number of threads to send queries.
    private static String serverHost = Helper.mustEnv("DB_HOST"); // hostname of TGraph (TCypher) server.
    private static boolean verifyResult = Boolean.parseBoolean(Helper.mustEnv("VERIFY_RESULT"));
    private static String resultFile = Helper.mustEnv("SERVER_RESULT_FILE");
    private static String writeStartTime = Helper.mustEnv("DATA_START");
    private static String writeEndTime = Helper.mustEnv("DATA_END");
    private static String startTime = Helper.mustEnv("TEMPORAL_DATA_START");
    private static String endTime = Helper.mustEnv("TEMPORAL_DATA_END");
    private static Integer testTravelTime = Integer.parseInt(Helper.mustEnv("TRAVEL_TIME"));
    private static String logTestName = Helper.mustEnv("LOG_TEST_NAME");
    private static String dataFilePath = Helper.mustEnv("RAW_DATA_PATH"); // should be like '/media/song/test/data-set/beijing-traffic/TGraph/byday/'
    private static String resultFilePath = Helper.mustEnv("RESULT_DATA_PATH");

    private static Producer logger;
    private static DBProxy clientWrite;
    private static DBProxy clientRead;
    private static BenchmarkTxResultProcessor post;

    @BeforeClass
    public static void init() throws IOException, ExecutionException, InterruptedException {
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
        clientWrite = new TGraphExecutorClient(serverHost, writeThreadCnt, 800);
        clientWrite.testServerClientCompatibility();
        clientRead = new TGraphExecutorClient(serverHost, readThreadCnt, 800);
        clientRead.testServerClientCompatibility();

        post = new BenchmarkTxResultProcessor(logTestName, Helper.codeGitVersion());
        logger = Helper.getLogger();
        post.setLogger(logger);
        post.setVerifyResult(verifyResult);
        post.setResult(new File(resultFilePath,resultFile));
    }

    @Test
    public void hybirdTest() throws Exception {

        wirteAppend(dataFilePath,writeStartTime,writeEndTime,1);
        for (int i=0;i<100;i++){
            readSnapshot("travel_time",Helper.timeStr2int(startTime));
        }
        for (int i=0;i<100;i++){
            readSnapshotAggrMax("travel_time",Helper.timeStr2int(startTime),Helper.timeStr2int(endTime));
        }
        for (int i=0;i<100;i++){
            readSnapshotAggrDuration("full_status",Helper.timeStr2int(startTime),Helper.timeStr2int(endTime));
        }
        for (int i=0;i<100;i++){
            readTemporalCondition("travel_time",Helper.timeStr2int(startTime),Helper.timeStr2int(endTime),1000);
        }
        for (int i=0;i<100;i++){
            readReachableArea(47294,Helper.timeStr2int(startTime),testTravelTime);
        }

    }

    private static void wirteAppend(String dataFilePath, String st, String et, int opPerTx) throws Exception {
        List<File> fileList = Helper.trafficFileList(dataFilePath, st, et);
        try(BenchmarkTxGenerator.TemporalPropertyAppendTxGenerator g = new BenchmarkTxGenerator.TemporalPropertyAppendTxGenerator(opPerTx, fileList)) {
            int i = 0;
            while (g.hasNext() && i<100) {
                AbstractTransaction tx = g.next();
                post.process(clientWrite.execute(tx), tx);
                i++;
            }
        }
    }

    private void readSnapshot(String propertyName, int t) throws Exception {
        SnapshotQueryTx tx = new SnapshotQueryTx();
        tx.setPropertyName(propertyName);
        tx.setTimestamp(t);
        post.process(clientRead.execute(tx), tx);
    }

    private void readSnapshotAggrMax(String propertyName, int st, int et) throws Exception {
        SnapshotAggrMaxTx tx = new SnapshotAggrMaxTx();
        tx.setP(propertyName);
        tx.setT0(st);
        tx.setT1(et);
        post.process(clientRead.execute(tx), tx);
    }

    private void readSnapshotAggrDuration(String propertyName, int st, int et) throws Exception {
        SnapshotAggrDurationTx tx = new SnapshotAggrDurationTx();
        tx.setP(propertyName);
        tx.setT0(st);
        tx.setT1(et);
        post.process(clientRead.execute(tx), tx);
    }

    private void readTemporalCondition(String PropertyName, int st, int et, int vMIN) throws Exception{
        EntityTemporalConditionTx tx = new EntityTemporalConditionTx();
        tx.setP(PropertyName);
        tx.setT0(st);
        tx.setT1(et);
        tx.setVmin(vMIN);
        post.process(clientRead.execute(tx), tx);
    }

    private void readReachableArea(long propertyName, int st, int tt) throws Exception {
        ReachableAreaQueryTx tx = new ReachableAreaQueryTx(propertyName,st,tt);
        tx.setStartCrossId(propertyName);
        tx.setDepartureTime(st);
        tx.setTravelTime(tt);
        post.process(clientRead.execute(tx), tx);
    }

    @AfterClass
    public static void close() throws IOException, InterruptedException, ProducerException {
        clientWrite.close();
        clientRead.close();
        while(true) {
            try {
                post.awaitDone(30, TimeUnit.SECONDS);
                break;
            } catch (InterruptedException ignored) {}
        }
        logger.close();
    }
}
