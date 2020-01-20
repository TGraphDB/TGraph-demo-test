package org.act.tgraph.demo.benchmark;

import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.aliyun.openservices.log.common.LogItem;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;
import org.act.tgraph.demo.benchmark.transaction.ReachableAreaQueryTx;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
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

    public void logMetrics(AbstractTransaction tx, JsonObject serverResponse) throws ProducerException, InterruptedException {
        JsonObject metrics = serverResponse.get("metrics").asObject();
        metrics.add("data_per_tx", tx.dataCount());
        LogItem log = new LogItem();
        log.PushBack("type", "time");
        add2LogItem(log, metrics);
        add2LogItem(log, serverResponse.get("server").asObject());
        logger.send("tgraph-demo-test", "tgraph-log", testName, clientVersion, log);
    }

    private void add2LogItem(LogItem log, JsonObject metrics) {
        for(JsonObject.Member m : metrics){
            log.PushBack(m.getName(), m.getValue().toString());
        }
    }

    public void validateResult(AbstractTransaction tx, JsonObject result){
        if(tx instanceof ReachableAreaQueryTx){
            validateResult(tx.getResult(), result);
        }
    }

    private void validateResult(JsonObject expected, JsonObject got){
        for(JsonObject.Member m : expected){
            JsonArray expArr = m.getValue().asArray();
            JsonArray gotArr = got.get(m.getName()).asArray();
            Preconditions.checkState(expArr.size()==gotArr.size());
            for(int i=0; i<expArr.size(); i++){
                Preconditions.checkState(Objects.equals(expArr.get(i), gotArr.get(i)));
            }
        }
    }

    public PostProcessing callback(AbstractTransaction tx){
        return new PostProcessing(tx);
    }

    public void process(ListenableFuture<JsonObject> result, AbstractTransaction tx) {
        Futures.addCallback( result, callback(tx), this.thread);
    }

    public class PostProcessing implements FutureCallback<JsonObject>{
        AbstractTransaction tx;
        PostProcessing(AbstractTransaction tx){
            this.tx = tx;
        }

        @Override
        public void onSuccess(@Nullable JsonObject result) {
            if(result==null) return;
            try {
                logMetrics(tx, result);
                if(verifyResult) validateResult(tx, result.get("result").asObject());
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
