package org.act.tgraph.demo.server;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.base.Preconditions;
import org.act.tgraph.demo.algo.EarliestArriveTime;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;
import org.act.tgraph.demo.benchmark.transaction.ImportStaticDataTx;
import org.act.tgraph.demo.benchmark.transaction.ImportTemporalDataTx;
import org.act.tgraph.demo.benchmark.transaction.ReachableAreaQueryTx;
import org.act.tgraph.demo.client.vo.RuntimeEnv;
import org.act.tgraph.demo.utils.Helper;
import org.act.tgraph.demo.utils.TGraphSocketServer;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.temporal.TimePoint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class KernelTcpServer extends TGraphSocketServer.ReqExecutor {
    public static void main(String[] args){
        TGraphSocketServer server = new TGraphSocketServer( dbDir(args), new KernelTcpServer() );
        RuntimeEnv env = RuntimeEnv.getCurrentEnv();
        String serverCodeVersion = env.name() + "." + Helper.codeGitVersion();
        System.out.println("server code version: "+ serverCodeVersion);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File dbDir(String[] args){
        if(args.length<1){
            throw new IllegalArgumentException("need arg: dbDir");
        }
        File dbDir = new File(args[0]);
        if( !dbDir.exists()){
            if(dbDir.mkdirs()) return dbDir;
            else throw new IllegalArgumentException("invalid dbDir");
        }else if( !dbDir.isDirectory()){
            throw new IllegalArgumentException("invalid dbDir");
        }
        return dbDir;
    }

    @Override
    protected JsonObject execute(String line) throws RuntimeException {
        JsonObject req = Json.parse(line).asObject();
        switch (AbstractTransaction.TxType.valueOf(req.get("type").asString())){
            case tx_import_static_data:
                execute(new ImportStaticDataTx(req));
                return Json.object();
            case tx_import_temporal_data:
                execute(new ImportTemporalDataTx(req));
                return Json.object();
            case tx_query_reachable_area:
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
                Preconditions.checkArgument(n.getId()==p.getLeft(), "id not match!");
            }
            for (ImportStaticDataTx.StaticRoadRel sr : tx.roads) {
                Node start = db.getNodeById(sr.startCrossId);
                Node end = db.getNodeById(sr.endCrossId);
                Relationship r = start.createRelationshipTo(end, RoadType.ROAD_TO);
                Preconditions.checkArgument(r.getId()==sr.roadId, "id not match");
            }
            t.success();
        }
    }


    private void execute(ImportTemporalDataTx tx) {
        try(Transaction t = db.beginTx()) {
            for(ImportTemporalDataTx.StatusUpdate s : tx.data){
                Relationship r = db.getRelationshipById(s.roadId);
                TimePoint time = Helper.time(s.time);
                r.setTemporalProperty("travel_time", time, s.travelTime);
                r.setTemporalProperty("full_status", time, s.jamStatus);
                r.setTemporalProperty("segment_count", time, s.segmentCount);
            }
            t.success();
        }
    }

    private JsonObject execute(ReachableAreaQueryTx tx){
        try(Transaction t = db.beginTx()) {
            EarliestArriveTime algo = new EarliestArriveTimeTGraphKernel(db, "travel_time", tx.startCrossId, tx.departureTime, tx.travelTime);
            List<EarliestArriveTime.NodeCross> result = new ArrayList<>(algo.run());
            result.sort(Comparator.comparingLong(o -> o.id));
            JsonObject res = Json.object();
            JsonArray nodeIdArr = Json.array();
            JsonArray arriveTimeArr = Json.array();
            JsonArray parentIdArr = Json.array();
            for(EarliestArriveTime.NodeCross node : result){
                nodeIdArr.add(node.id);
                arriveTimeArr.add(node.arriveTime);
                parentIdArr.add(node.parent);
            }
            res.add("nodeId", nodeIdArr);
            res.add("arriveTime", arriveTimeArr);
            res.add("parentId", parentIdArr);
            t.failure();
            return res;
        }
    }
}
