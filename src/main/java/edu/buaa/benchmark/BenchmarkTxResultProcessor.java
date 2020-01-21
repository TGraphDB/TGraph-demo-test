package org.act.tgraph.demo.benchmark;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.aliyun.openservices.log.common.LogItem;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.act.tgraph.demo.benchmark.client.DBProxy;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction.Metrics;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BenchmarkTxResultProcessor {
    public final Executor thread = Executors.newSingleThreadExecutor();
    private final Producer logger;
    private final String testName;
    private final String clientVersion;
    private final boolean verifyResult;

    public BenchmarkTxResultProcessor(Producer logger, String testName, String clientVersion, boolean verifyResult){
        this.logger = logger;
        this.testName = testName;
        this.clientVersion = clientVersion;
        this.verifyResult = verifyResult;
    }

    public void logMetrics(AbstractTransaction tx, DBProxy.ServerResponse response) throws ProducerException, InterruptedException {
        Metrics m = response.getMetrics();
        LogItem log = new LogItem();
        add2LogItem(log, (JSONObject)JSON.toJSON(m));
        logger.send("tgraph-demo-test", "tgraph-log", testName, clientVersion, log);
    }

    private void add2LogItem(LogItem log, JSONObject metrics) {
        for(Map.Entry<String, Object> e : metrics.entrySet()){
            log.PushBack(e.getKey(), e.getValue().toString());
        }
    }

    public PostProcessing callback(AbstractTransaction tx){
        return new PostProcessing(tx);
    }

    public void process(ListenableFuture<DBProxy.ServerResponse> result, AbstractTransaction tx) {
        Futures.addCallback( result, callback(tx), this.thread);
    }

    public class PostProcessing implements FutureCallback<DBProxy.ServerResponse>{
        AbstractTransaction tx;
        PostProcessing(AbstractTransaction tx){
            this.tx = tx;
        }

        @Override
        public void onSuccess(DBProxy.ServerResponse result) {
            if(result==null) return;
            try {
                logMetrics(tx, result);
                if(verifyResult) tx.validateResult(result.getResult());
            } catch (ProducerException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(Throwable t) {
            t.printStackTrace();
        }
    }

}
