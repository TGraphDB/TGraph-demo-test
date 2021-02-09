package edu.buaa.benchmark.client;

import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.ListenableFuture;
import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.benchmark.transaction.AbstractTransaction.Metrics;
import edu.buaa.benchmark.transaction.AbstractTransaction.Result;

import java.io.IOException;

/**
 * Created by song on 16-2-23.
 */
public interface DBProxy {
    // return server version.
    String testServerClientCompatibility() throws UnsupportedOperationException;

    ListenableFuture<ServerResponse> execute(AbstractTransaction tx) throws Exception;

    void createDB() throws IOException;

    // restart database. blocking until service available (return true) or failed (return false).
    void restartDB() throws IOException;

    void shutdownDB() throws IOException;

    void close() throws IOException, InterruptedException;

    class ServerResponse{
        private Result result;
        private Metrics metrics;

        public Result getResult() {
            return result;
        }

        public void setResult(Result result) { this.result = result; }

        public Metrics getMetrics() {
            return metrics;
        }

        public void setMetrics(Metrics metrics) {
            this.metrics = metrics;
        }
    }
}
