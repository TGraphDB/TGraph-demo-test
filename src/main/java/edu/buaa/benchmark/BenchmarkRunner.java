package edu.buaa.benchmark;

import com.aliyun.openservices.aliyun.log.producer.Producer;
import edu.buaa.benchmark.client.DBProxy;
import edu.buaa.benchmark.client.SqlServerExecutorClient;
import edu.buaa.benchmark.client.TGraphExecutorClient;
import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.utils.Helper;

import java.io.File;
import java.util.Calendar;

import static edu.buaa.benchmark.transaction.AbstractTransaction.TxType.*;

public class BenchmarkRunner {

    public static void main(String[] args) {
        // fast debug use.
        test();
        System.exit(0);
        String benchmarkFileName = Helper.mustEnv("BENCHMARK_FILE_INPUT");
        String dbType = Helper.mustEnv("DB_TYPE");
        int maxConnCnt = Integer.parseInt(Helper.mustEnv("MAX_CONNECTION_CNT"));
        String dbHost = Helper.mustEnv("DB_HOST");
        boolean verifyResult = Boolean.parseBoolean(Helper.mustEnv("VERIFY_RESULT"));

        try {
            DBProxy client;
            switch (dbType.toLowerCase()) {
                case "tgraph_kernel":
                    client = new TGraphExecutorClient(dbHost, maxConnCnt, 800);
                    break;
                case "sql_server":
                    client = new SqlServerExecutorClient(dbHost, maxConnCnt, 800);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            String serverVersion = client.testServerClientCompatibility();
            Producer logger = Helper.getLogger();
            BenchmarkTxResultProcessor post = new BenchmarkTxResultProcessor(logger, getTestName(dbType, serverVersion), Helper.codeGitVersion(), verifyResult);
            client.createDB();
            BenchmarkReader reader = new BenchmarkReader(new File(benchmarkFileName));
            while (reader.hasNext()) {
                AbstractTransaction tx = reader.next();
                post.process(client.execute(tx), tx);
            }
            reader.close();
            client.close();
            logger.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getTestName(String dbType, String dbVersion){
        Calendar c = Calendar.getInstance();
        return "B_"+dbType.toLowerCase()+"("+dbVersion+")_"+
                c.get(Calendar.YEAR)+"."+(c.get(Calendar.MONTH)+1)+"."+c.get(Calendar.DAY_OF_MONTH)+"_"+
                c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE);
    }

    private static void test(){
        try {
            //DBProxy client = new TGraphExecutorClient("localhost", 1, 800);
            DBProxy client = new SqlServerExecutorClient("39.96.57.88", 1, 800);
            String serverVersion = client.testServerClientCompatibility();
            Producer logger = Helper.getLogger();
            BenchmarkTxResultProcessor post = new BenchmarkTxResultProcessor(logger, getTestName("sql_server", serverVersion), Helper.codeGitVersion(), true);
            //BenchmarkTxResultProcessor post = new BenchmarkTxResultProcessor(logger, getTestName("tgraph_kernel", serverVersion), Helper.codeGitVersion(), true);
            client.createDB();
            BenchmarkReader reader = new BenchmarkReader(new File("e:/tgraph/test-data/benchmark-with-result.gz"));
            while (reader.hasNext()) {
                AbstractTransaction tx = reader.next();
                if(tx.getTxType()==tx_query_road_earliest_arrive_time_aggr) post.process(client.execute(tx), tx);
            }
            reader.close();
            client.close();
            logger.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
