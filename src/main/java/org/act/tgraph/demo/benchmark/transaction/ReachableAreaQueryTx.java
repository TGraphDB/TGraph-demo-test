package org.act.tgraph.demo.benchmark.transaction;

import com.eclipsesource.json.JsonObject;

public class ReachableAreaQueryTx extends AbstractTransaction {
    public final long startCrossId;
    public final int departureTime;
    public final int travelTime;

    public ReachableAreaQueryTx(long startCrossId, int departureTime, int travelTime){
        this.startCrossId = startCrossId;
        this.departureTime = departureTime;
        this.travelTime = travelTime;
    }

    public ReachableAreaQueryTx(JsonObject o) {
        assert TxType.valueOf(o.get("type").asString()) == TxType.tx_query_reachable_area;
        this.startCrossId = o.get("startCrossId").asLong();
        this.departureTime = o.get("departureTime").asInt();
        this.travelTime = o.get("travelTime").asInt();
    }

    @Override
    public JsonObject encodeArgs() {
        JsonObject obj = newTx(TxType.tx_query_reachable_area);
        obj.add("startCrossId", startCrossId);
        obj.add("departureTime", departureTime);
        obj.add("travelTime", travelTime);
        return obj;
    }


}
