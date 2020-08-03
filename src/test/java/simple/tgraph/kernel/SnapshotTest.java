package simple.tgraph.kernel;


import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import edu.buaa.benchmark.BenchmarkTxResultProcessor;
import edu.buaa.benchmark.client.DBProxy;
import edu.buaa.benchmark.client.TGraphExecutorClient;
import edu.buaa.benchmark.transaction.SnapshotQueryTx;
import edu.buaa.utils.Helper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class SnapshotTest {

    private static int threadCnt = 10; // number of threads to send queries.
    private static String serverHost = "tgraph.server"; // hostname of TGraph (TCypher) server.
    private static boolean verifyResult = true;

    private static Producer logger;
    private static DBProxy client;
    private static BenchmarkTxResultProcessor post;

    @BeforeClass
    public static void init() throws IOException, ExecutionException, InterruptedException {
        logger = Helper.getLogger();
        client = new TGraphExecutorClient(serverHost, threadCnt, 800);
        client.testServerClientCompatibility();
        post = new BenchmarkTxResultProcessor(logger, "TGraph(Snapshot)", Helper.codeGitVersion(), verifyResult);
    }

    @Test
    public void jam06300940() throws Exception {
        query("full_status", Helper.timeStr2int("201006300940"));
    }

    @Test
    public void travelTime06300940() throws Exception {
        query("travel_time", Helper.timeStr2int("201006300940"));
    }

    private void query(String propertyName, int t) throws Exception {
        for(int i=0; i<10000; i++){
            SnapshotQueryTx tx = new SnapshotQueryTx();
            tx.setPropertyName(propertyName);
            tx.setTimestamp(t);
            post.process(client.execute(tx), tx);
        }
    }

    @AfterClass
    public static void close() throws IOException, InterruptedException, ProducerException {
        client.close();
        logger.close();
    }

}
