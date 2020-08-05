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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class WriteTemporalPropertyTest {

    private static int threadCnt = Integer.parseInt(Helper.mustEnv("MAX_CONNECTION_CNT")); // number of threads to send queries.
    private static int opPerTx = Integer.parseInt(Helper.mustEnv("TEMPORAL_DATA_PER_TX")); // number of TCypher queries executed in one transaction.
    private static String startDay = Helper.mustEnv("TEMPORAL_DATA_START"); //0501
    private static String endDay = Helper.mustEnv("TEMPORAL_DATA_END"); //0503
    private static String serverHost = Helper.mustEnv("DB_HOST"); // hostname of TGraph (TCypher) server.
    private static String dataFilePath = Helper.mustEnv("RAW_DATA_PATH"); // should be like '/media/song/test/data-set/beijing-traffic/TGraph/byday/'

    private static Producer logger;
    private static DBProxy client;
    private static BenchmarkTxResultProcessor post;

    @BeforeClass
    public static void initClient() throws IOException, ExecutionException, InterruptedException {
        logger = Helper.getLogger();
        client = new TGraphExecutorClient(serverHost, threadCnt, 800);
        client.testServerClientCompatibility();
        post = new BenchmarkTxResultProcessor("TGraph(TpWrt)", Helper.codeGitVersion());
        post.setLogger(logger);
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
