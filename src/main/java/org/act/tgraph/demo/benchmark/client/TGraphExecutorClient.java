package org.act.tgraph.demo.benchmark.client;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.util.concurrent.Futures;
import org.act.tgraph.demo.benchmark.BenchmarkTxResultProcessor;
import org.act.tgraph.demo.benchmark.DBOperation;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;
import org.act.tgraph.demo.client.TGraphTcpConnection;
import org.act.tgraph.demo.client.TGraphSocketClient;
import org.act.tgraph.demo.utils.TimeMonitor;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

public class TGraphExecutorClient implements DBOperation {
    private BenchmarkTxResultProcessor processor;
    private BlockingQueue<TGraphTcpConnection> connectionPool = new LinkedBlockingQueue<>();
    private TGraphSocketClient client;

    public TGraphExecutorClient(String serverHost, int parallelCnt, int queueLength, BenchmarkTxResultProcessor processor) throws IOException, ExecutionException, InterruptedException {
        this.client = new CustomClient(serverHost, parallelCnt, queueLength);
        this.processor = processor;
    }

    @Override
    public void createDB() throws IOException {}

    @Override
    public void restartDB() throws IOException {

    }

    @Override
    public void shutdownDB() throws IOException {

    }

    public void execute(AbstractTransaction tx){
        String req = tx.encode();
        Futures.addCallback(client.addQuery(req), processor.callback(tx), processor.thread);
    }

    public void close() throws IOException, InterruptedException {
        client.awaitTermination();
    }

    private static class CustomClient extends TGraphSocketClient{
        CustomClient(String serverHost, int parallelCnt, int queueLength) throws IOException, ExecutionException, InterruptedException {
            super(serverHost, parallelCnt, queueLength);
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

}
