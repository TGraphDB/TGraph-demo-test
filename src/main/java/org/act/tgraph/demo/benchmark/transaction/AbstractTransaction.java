package org.act.tgraph.demo.benchmark.transaction;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public abstract class AbstractTransaction {
    public enum TxType{
        tx_import_static_data,
        tx_import_temporal_data,
        tx_query_reachable_area;
    }
    private JsonObject result;
    private JsonObject metrics;

    public abstract JsonObject encodeArgs();

    public JsonObject encodeResult(){
        return result==null ? new JsonObject() : result;
    }

    public void setResult(JsonObject result){
        this.result = result;
    }

    public JsonObject encodeMetrics(){
        return metrics==null ? new JsonObject() : metrics;
    }

    public void setMetrics(JsonObject metrics){
        this.metrics = metrics;
    }

    public JsonObject getResult() {
        return result;
    }

    public JsonObject getMetrics() {
        return metrics;
    }

    protected JsonObject newTx(TxType type){
        JsonObject obj = new JsonObject();
        obj.add("type", type.name());
        return obj;
    }

    public abstract String encode();
}
