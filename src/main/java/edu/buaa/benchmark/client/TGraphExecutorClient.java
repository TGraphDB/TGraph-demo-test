package edu.buaa.benchmark.client;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.ListenableFuture;
import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.client.TGraphSocketClient;
import edu.buaa.utils.Helper;
import edu.buaa.utils.TimeMonitor;

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
    public ListenableFuture<ServerResponse> execute(AbstractTransaction tx) throws Exception{
        return this.addQuery(JSON.toJSONString(tx, Helper.serializerFeatures));
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
    protected ServerResponse onResponse(String query, String response, TimeMonitor timeMonitor, Thread thread) throws Exception {
        ServerResponse res = JSON.parseObject(response, ServerResponse.class);
        AbstractTransaction.Metrics metrics = res.getMetrics();
        metrics.setConnId(Math.toIntExact(Thread.currentThread().getId()));
        metrics.setExeTime(Math.toIntExact(timeMonitor.duration("Send query") + timeMonitor.duration("Wait result")));
        metrics.setWaitTime(Math.toIntExact(timeMonitor.duration("Wait in queue")));
        metrics.setSendTime(timeMonitor.beginT("Send query"));
        metrics.setReqSize(query.length());
        metrics.setReturnSize(response.length());
        return res;
    }

}
