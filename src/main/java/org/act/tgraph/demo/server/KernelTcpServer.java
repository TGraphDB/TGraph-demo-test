package org.act.tgraph.demo.server;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import org.act.tgraph.demo.algo.EarliestArriveTime;
import org.act.tgraph.demo.benchmark.transaction.ImportStaticDataTx;
import org.act.tgraph.demo.benchmark.transaction.ImportTemporalDataTx;
import org.act.tgraph.demo.benchmark.transaction.ReachableAreaQueryTx;
import org.act.tgraph.demo.client.vo.RuntimeEnv;
import org.act.tgraph.demo.utils.TGraphSocketServer;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.temporal.TimePoint;

import java.io.IOException;
import java.util.Set;

public class KernelTcpServer extends TGraphSocketServer.ReqExecutor {
    public static void main(String[] args){
        TGraphSocketServer server = new TGraphSocketServer(RuntimeEnv.getCurrentEnv().getConf().dbPath, new KernelTcpServer());
        RuntimeEnv env = RuntimeEnv.getCurrentEnv();
        String serverCodeVersion = env.name() + "." + env.getConf().codeGitVersion();
        System.out.println("server code version: "+ serverCodeVersion);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected JsonObject execute(String line) throws RuntimeException {
        JsonObject req = Json.parse(line).asObject();
        switch (req.get("type").asString()){
            case "tx_import_static_data":
                execute(new ImportStaticDataTx(req));
                return Json.object();
            case "tx_import_temporal_data":
                execute(new ImportTemporalDataTx(req));
                return Json.object();
            case "tx_reachable_area_query":
                return execute(new ReachableAreaQueryTx(req));
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void execute(ImportStaticDataTx tx){
        try(Transaction t = db.beginTx()) {
            for (Pair<Long, String> p : tx.crosses) {
                Node n = db.createNode();
                n.setProperty("name", p.getRight());
                assert n.getId()==p.getLeft();
            }
            for (ImportStaticDataTx.StaticRoadRel sr : tx.roads) {
                Node start = db.getNodeById(sr.startCrossId);
                Node end = db.getNodeById(sr.endCrossId);
                Relationship r = start.createRelationshipTo(end, RoadType.ROAD_TO);
                assert r.getId()==sr.roadId;
            }
            t.success();
        }
    }


    private void execute(ImportTemporalDataTx tx) {
        try(Transaction t = db.beginTx()) {
            for(ImportTemporalDataTx.StatusUpdate s : tx.data){
                Relationship r = db.getRelationshipById(s.roadId);
                r.setTemporalProperty("travel_time", TimePoint.unix(s.time), s.travelTime);
                r.setTemporalProperty("full_status", TimePoint.unix(s.time), s.jamStatus);
                r.setTemporalProperty("segment_count", TimePoint.unix(s.time), s.segmentCount);
            }
            t.success();
        }
    }

    private JsonObject execute(ReachableAreaQueryTx tx){
        try(Transaction t = db.beginTx()) {
            EarliestArriveTime algo = new EarliestArriveTimeTGraphKernel(db, "travel_time", tx.startCrossId, tx.departureTime, tx.travelTime);
            Set<EarliestArriveTime.NodeCross> result = algo.run();
            JsonObject res = Json.object();
            JsonArray nodeIdArr = Json.array();
            JsonArray arriveTimeArr = Json.array();
            JsonArray parentIdArr = Json.array();
            for(EarliestArriveTime.NodeCross node : result){
                nodeIdArr.add(node.id);
                arriveTimeArr.add(node.arriveTime);
                parentIdArr.add(node.parent.id);
            }
            res.add("nodeId", nodeIdArr);
            res.add("arriveTime", arriveTimeArr);
            res.add("parentId", parentIdArr);
            t.failure();
            return res;
        }
    }
}
