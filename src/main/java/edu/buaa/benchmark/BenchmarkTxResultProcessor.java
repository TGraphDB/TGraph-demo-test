package edu.buaa.benchmark;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.aliyun.openservices.log.common.LogItem;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import edu.buaa.benchmark.client.DBProxy;
import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.benchmark.transaction.AbstractTransaction.Metrics;

import java.util.Map;
import java.util.concurrent.ExecutionException;
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
        JSONObject mObj = mergeMetrics(response.getMetrics(), tx.getMetrics());
        LogItem log = new LogItem();
        log.PushBack("type", tx.getTxType().name());
        add2LogItem(log, mObj);
        logger.send("tgraph-demo-test", "tgraph-log", testName, clientVersion, log);
    }

    private JSONObject mergeMetrics(Metrics mFromClient, Metrics mFromTx) {
        if(mFromTx!=null){
            if(mFromTx.getReqSize()>0){
                mFromClient.setReqSize(mFromTx.getReqSize());
            }
            if(mFromTx.getReturnSize()>0){
                mFromClient.setReqSize(mFromTx.getReturnSize());
            }
        }
        return (JSONObject) JSON.toJSON(mFromClient);
    }

    private void add2LogItem(LogItem log, JSONObject metrics) {
        for(Map.Entry<String, Object> e : metrics.entrySet()){
            if(e.getValue() instanceof JSONObject){
                JSONObject v = (JSONObject) e.getValue();
                for(Map.Entry<String, Object> ee : v.entrySet()){
                    log.PushBack(e.getKey()+"_"+ee.getKey(), ee.getValue().toString());
                }
            }else {
                log.PushBack(e.getKey(), e.getValue().toString());
            }
        }
    }


    public void process(ListenableFuture<DBProxy.ServerResponse> result, AbstractTransaction tx) {
        Futures.addCallback( result, new PostProcessing(tx), this.thread);
//        try {
//            callback(tx).onSuccess(result.get());
//        } catch (InterruptedException | ExecutionException e) {
//            e.printStackTrace();
//        }
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
