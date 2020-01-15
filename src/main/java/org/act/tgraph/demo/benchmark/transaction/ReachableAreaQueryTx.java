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

    public ReachableAreaQueryTx(JsonObject obj) {
        assert obj.get("type").asString().equals("tx_reachable_area_query");
        JsonObject o = obj.get("args").asObject();
        this.startCrossId = o.get("startCrossId").asLong();
        this.departureTime = o.get("departureTime").asInt();
        this.travelTime = o.get("travelTime").asInt();
    }

    @Override
    public JsonObject encodeArgs() {
        JsonObject obj = newTx("tx_reachable_area_query");
        JsonObject args = new JsonObject();
        args.add("startCrossId", startCrossId);
        args.add("departureTime", departureTime);
        args.add("travelTime", travelTime);
        obj.add("args", args);
        return obj;
    }
}
