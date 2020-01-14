package org.act.tgraph.demo.benchmark.transaction;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.act.tgraph.demo.benchmark.ResultChecker;
import org.act.tgraph.demo.client.driver.DBOperationProxy;
import org.act.tgraph.demo.model.CrossNode;
import org.act.tgraph.demo.model.RoadRel;
import org.act.tgraph.demo.model.TrafficTemporalPropertyGraph;
import org.act.tgraph.demo.utils.LoggerProxy;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportStaticDataTx implements AbstractTransaction {
    private final List<Pair<Long, String>> crosses;
    private final List<StaticRoadRel> roads;

    // this constructor is used when generating benchmarks.
    public ImportStaticDataTx(List<Pair<Long, String>> crosses, List<StaticRoadRel> roads) {
        this.crosses = crosses;
        this.roads = roads;
    }

    public ImportStaticDataTx(String lineContent){
        crosses = new ArrayList<>();
        roads = new ArrayList<>();
        JsonObject obj = Json.parse(lineContent).asObject();
        JsonArray crossArr = obj.get("cross").asArray();
        for(JsonValue v : crossArr){
            JsonObject c = v.asObject();
            crosses.add(Pair.of(c.get("id").asLong(), c.get("name").asString()));
        }
        for(JsonValue v : obj.get("road").asArray()){
            JsonObject r = v.asObject();
            roads.add(new StaticRoadRel(r));
        }
    }

    @Override
    public boolean execute(LoggerProxy logger, DBOperationProxy db, ResultChecker checker) {
        DBOperationProxy.TxProxy tx = db.beginTx();
        for(Pair<Long, String> cross : crosses){
            long cid = tx.createNode(cross.getRight());
            checker.checkEqual(cid, cross.getLeft());
        }
        for(StaticRoadRel r : roads){
            long rid = tx.createRel(r.startCrossId, r.endCrossId, r.id, r.length, r.type, r.angle);
            checker.checkEqual(rid, r.roadId);
        }
        tx.commit();
        return true;
    }

    @Override
    public String encode() {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        StringBuilder sb = new StringBuilder();
        for(Pair<Long, String> cross : crosses){
            JsonObject c = new JsonObject();
            c.add("id", cross.getLeft());
            c.add("name", cross.getRight());
            arr.add(c);
        }
        obj.add("cross", arr);
        JsonArray roadArr = new JsonArray();
        for(StaticRoadRel r : roads){
            roadArr.add(r.encode());
        }
        obj.add("road", roadArr);
        return obj.toString();
    }

    public static class StaticRoadRel{
        final long roadId, startCrossId, endCrossId;
        final String id;
        final int length, angle, type;
        public StaticRoadRel(long roadId, long startCrossId, long endCrossId, String id, int length, int angle, int type) {
            this.roadId = roadId;
            this.startCrossId = startCrossId;
            this.endCrossId = endCrossId;
            this.id = id;
            this.length = length;
            this.angle = angle;
            this.type = type;
        }

        StaticRoadRel(JsonObject obj) {
            this.roadId = obj.get("id").asLong();
            this.startCrossId = obj.get("start").asLong();
            this.endCrossId = obj.get("end").asLong();
            this.id = obj.get("name").asString();
            this.length = obj.get("length").asInt();
            this.angle = obj.get("angle").asInt();
            this.type = obj.get("type").asInt();
        }

        JsonObject encode(){
            JsonObject obj = new JsonObject();
            obj.add("id", roadId);
            obj.add("start", startCrossId);
            obj.add("end", endCrossId);
            obj.add("name", id);
            obj.add("length", length);
            obj.add("type", type);
            obj.add("angle", angle);
            return obj;
        }
    }
}
