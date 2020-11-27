package simple.tgraph.kernel.index;

import com.alibaba.fastjson.parser.ParserConfig;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import edu.buaa.benchmark.BenchmarkTxResultProcessor;
import edu.buaa.benchmark.client.DBProxy;
import edu.buaa.benchmark.client.TGraphExecutorClient;
import edu.buaa.benchmark.transaction.SnapshotAggrMaxTx;
import edu.buaa.benchmark.transaction.index.CreateTGraphAggrMaxIndexTx;
import edu.buaa.benchmark.transaction.index.SnapshotAggrMaxIndexTx;
import edu.buaa.utils.Helper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SnapshotAggregationMaxIndexTest {
    private static int threadCnt = 1; // number of threads to send queries.
    private static String serverHost = Helper.mustEnv("DB_HOST"); // hostname of TGraph (TCypher) server.
    private static boolean verifyResult = true;

    private static Producer logger;
    private static DBProxy client;
    private static BenchmarkTxResultProcessor post;

    @BeforeClass
    public static void init() throws IOException, ExecutionException, InterruptedException {
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
        client = new TGraphExecutorClient(serverHost, threadCnt, 800);
        client.testServerClientCompatibility();
        post = new BenchmarkTxResultProcessor("TGraph(SnapshotAggregationMaxIndexTest)", Helper.codeGitVersion());
//        logger = Helper.getLogger();
//        post.setLogger(logger);
        post.setVerifyResult(verifyResult);
    }

    @Test
    public void createIndex() throws Exception {
        CreateTGraphAggrMaxIndexTx tx = new CreateTGraphAggrMaxIndexTx();
        tx.setProName("travel_time");
        tx.setStart(Helper.timeStr2int("201006280000"));
        tx.setEnd(Helper.timeStr2int("201006300000"));
        tx.setEvery(1);
        tx.setTimeUnit(Calendar.HOUR);
        post.process(client.execute(tx), tx);
    }

//    @Test
//    public void queryByIndex() throws Exception{
//        SnapshotAggrMaxIndexTx tx = new SnapshotAggrMaxIndexTx();
//        tx.setP("travel_time");
//        tx.setIndexId();
//    }

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
