package org.act.tgraph.demo.benchmark.client;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.util.concurrent.ListenableFuture;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;
import org.act.tgraph.demo.client.TGraphSocketClient;
import org.act.tgraph.demo.utils.TimeMonitor;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class TGraphExecutorClient extends TGraphSocketClient implements DBProxy {

    public TGraphExecutorClient(String serverHost, int parallelCnt, int queueLength) throws IOException, ExecutionException, InterruptedException {
        super(serverHost, parallelCnt, queueLength);
    }

    @Override
    public void createDB() throws IOException {}

    @Override
    public void restartDB() throws IOException {

    }

    @Override
    public void shutdownDB() throws IOException {

    }

    @Override
    public ListenableFuture<JsonObject> execute(AbstractTransaction tx) throws Exception{
        String req = tx.encode();
        return this.addQuery(req);
    }

    @Override
    public String testServerClientCompatibility() {
        try {
            return super.testServerClientCompatibility();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }

    public void close() throws IOException, InterruptedException {
        this.awaitTermination();
    }

    @Override
    protected JsonObject onResponse(String query, String response, TimeMonitor timeMonitor, Thread thread) throws Exception {
        JsonObject res = Json.parse(response).asObject();
        JsonObject metrics = res.get("metrics").asObject();
        metrics.add("thread", "T." + Thread.currentThread().getId());
        metrics.add("queue_tD", timeMonitor.duration("Wait in queue"));
        metrics.add("send_t", timeMonitor.beginT("Send query"));
        metrics.add("send_tD", timeMonitor.duration("Send query"));
        metrics.add("wait_tD", timeMonitor.duration("Wait result"));
        metrics.add("request_size", query.length());
        metrics.add("response_size", response.length());
        return res;
    }

}
