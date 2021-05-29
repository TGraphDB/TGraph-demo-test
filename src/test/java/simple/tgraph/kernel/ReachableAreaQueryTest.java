package simple.tgraph.kernel;

import com.alibaba.fastjson.parser.ParserConfig;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.google.common.base.Preconditions;
import edu.buaa.benchmark.BenchmarkTxResultProcessor;
import edu.buaa.benchmark.client.DBProxy;
import edu.buaa.benchmark.client.TGraphExecutorClient;
import edu.buaa.benchmark.transaction.ImportStaticDataTx;
import edu.buaa.benchmark.transaction.ReachableAreaQueryTx;
import edu.buaa.utils.Helper;
import org.act.temporalProperty.index.value.IndexMetaData;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ReachableAreaQueryTest {
    private static int threadCnt = Integer.parseInt(Helper.mustEnv("MAX_CONNECTION_CNT")); // number of threads to send queries.
    private static String serverHost = Helper.mustEnv("DB_HOST"); // hostname of TGraph (TCypher) server.
    private static boolean verifyResult = Boolean.parseBoolean(Helper.mustEnv("VERIFY_RESULT"));
    private static String resultFile = Helper.mustEnv("SERVER_RESULT_FILE");
    private static String dataFilePath = Helper.mustEnv("RESULT_DATA_PATH");
    private static String startTime = Helper.mustEnv("TEMPORAL_DATA_START");
    private static Integer testTravelTime = Integer.parseInt(Helper.mustEnv("TRAVEL_TIME"));
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
    public void reachableAreaQueryInfo() throws Exception {
        for(int i=0;i<200;i++) {
            query(47294, Helper.timeStr2int(startTime), testTravelTime);
        }
    }

    private void query(long propertyName, int st, int tt) throws Exception {
            ReachableAreaQueryTx tx = new ReachableAreaQueryTx(propertyName,st,tt);
            tx.setStartCrossId(propertyName);
            tx.setDepartureTime(st);
            tx.setTravelTime(tt);
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
