package simple.tgraph.kernel;


import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import edu.buaa.benchmark.BenchmarkTxGenerator;
import edu.buaa.benchmark.BenchmarkTxResultProcessor;
import edu.buaa.benchmark.client.DBProxy;
import edu.buaa.benchmark.client.TGraphExecutorClient;
import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.utils.Helper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RunWith(Parameterized.class)
public class WriteTemporalPropertyTest {

    private static int threadCnt = 10; // number of threads to send queries.
    private static int opPerTx = 1000; // number of TCypher queries executed in one transaction.
    private static String startDay = "0501";
    private static String endDay = "0503";
    private static String serverHost = "tgraph.server"; // hostname of TGraph (TCypher) server.
    private static String dataFilePath = "/tmp/traffic"; // should be like '/media/song/test/data-set/beijing-traffic/TGraph/byday/'

    private static Producer logger;
    private static DBProxy client;
    private static BenchmarkTxResultProcessor post;

    @BeforeClass
    public static void initClient() throws IOException, ExecutionException, InterruptedException {
        logger = Helper.getLogger();
        client = new TGraphExecutorClient(serverHost, threadCnt, 800);
        client.testServerClientCompatibility();
        post = new BenchmarkTxResultProcessor(logger, "TGraph(TpWrt)", Helper.codeGitVersion(), false);
    }

    @Test
    public void run() throws Exception {
        List<File> fileList = Helper.trafficFileList(dataFilePath, startDay, endDay);
        try(BenchmarkTxGenerator.TemporalPropertyAppendTxGenerator g = new BenchmarkTxGenerator.TemporalPropertyAppendTxGenerator(opPerTx, fileList)) {
            while (g.hasNext()) {
                AbstractTransaction tx = g.next();
                post.process(client.execute(tx), tx);
            }
        }
    }

    @AfterClass
    public static void close() throws IOException, InterruptedException, ProducerException {
        client.close();
        logger.close();
    }

}
