package org.act.tgraph.demo.benchmark.transaction;

import com.eclipsesource.json.JsonObject;

public abstract class AbstractTransaction {

    public abstract JsonObject encodeArgs();

    public JsonObject encodeMetrics(){
        return new JsonObject();
    }

    protected JsonObject newTx(String type){
        JsonObject obj = new JsonObject();
        obj.add("type", type);
        return obj;
    }

}
