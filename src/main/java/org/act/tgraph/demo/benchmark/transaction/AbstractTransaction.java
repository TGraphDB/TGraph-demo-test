package org.act.tgraph.demo.benchmark.transaction;

import com.eclipsesource.json.JsonObject;

public abstract class AbstractTransaction {
    public enum TxType{
        tx_import_static_data,
        tx_import_temporal_data,
        tx_query_reachable_area;
    }
    public final TxType txType;
    private JsonObject result;
    private JsonObject metrics;

    protected AbstractTransaction(TxType txType) {
        this.txType = txType;
    }

    public abstract String encode();

    // return 1 for queries, override in write tx.
    public int dataCount(){
        return 1;
    }

    public void setResult(JsonObject result){
        this.result = result;
    }

    public void setMetrics(JsonObject metrics){
        this.metrics = metrics;
    }

    public JsonObject getResult() {
        return result==null ? new JsonObject() : result;
    }

    public JsonObject getMetrics() {
        return metrics==null ? new JsonObject() : metrics;
    }

    protected JsonObject newTx(TxType type){
        JsonObject obj = new JsonObject();
        obj.add("type", type.name());
        return obj;
    }
}
