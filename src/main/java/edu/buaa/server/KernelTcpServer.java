package edu.buaa.server;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import edu.buaa.algo.EarliestArriveTime;
import edu.buaa.benchmark.transaction.*;
import edu.buaa.benchmark.transaction.AbstractTransaction.Result;
import edu.buaa.client.vo.RuntimeEnv;
import edu.buaa.utils.Helper;
import edu.buaa.utils.TGraphSocketServer;
import org.act.temporalProperty.impl.InternalEntry;
import org.act.temporalProperty.impl.InternalKey;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.temporal.TemporalRangeQuery;
import org.neo4j.temporal.TimePoint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KernelTcpServer extends TGraphSocketServer.ReqExecutor {
    public static void main(String[] args){
        TGraphSocketServer server = new TGraphSocketServer( dbDir(), new KernelTcpServer() );
        RuntimeEnv env = RuntimeEnv.getCurrentEnv();
        String serverCodeVersion = env.name() + "." + Helper.codeGitVersion();
        System.out.println("server code version: "+ serverCodeVersion);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File dbDir(){
        String path = Helper.mustEnv("DB_PATH");
        Preconditions.checkNotNull(path, "need arg: DB_PATH");
        File dbDir = new File(path);
        if( !dbDir.exists()){
            if(dbDir.mkdirs()) return dbDir;
            else throw new IllegalArgumentException("invalid dbDir");
        }else if( !dbDir.isDirectory()){
            throw new IllegalArgumentException("invalid dbDir");
        }
        return dbDir;
    }

    @Override
    protected Result execute(String line) throws RuntimeException {
        AbstractTransaction tx = JSON.parseObject(line, AbstractTransaction.class);
        switch (tx.getTxType()){
            case tx_import_static_data: return execute((ImportStaticDataTx) tx);
            case tx_import_temporal_data: return execute((ImportTemporalDataTx) tx);
            case tx_query_reachable_area: return execute((ReachableAreaQueryTx) tx);
            case tx_query_road_earliest_arrive_time_aggr: return execute((EarliestArriveTimeAggrTx)tx);
            case tx_query_node_neighbor_road: return execute((NodeNeighborRoadTx) tx);
            default:
                throw new UnsupportedOperationException();
        }
    }

    private Result execute(EarliestArriveTimeAggrTx tx) {
        EarliestArriveTimeAggrTx.Result result = new EarliestArriveTimeAggrTx.Result();
        try(Transaction t = db.beginTx()) {
            Relationship r = db.getRelationshipById(tx.getRoadId());
            if (!r.hasProperty("travel_time")) {
                result.setArriveTime(-1);
                return result;
            }
            Object tObj = r.getTemporalProperty("travel_time", Helper.time(tx.getDepartureTime()), Helper.time(tx.getEndTime()), new TemporalRangeQuery() {
                @Override public void setValueType(String valueType) { }
                private int minArriveT = Integer.MAX_VALUE;
                @Override
                public void onNewEntry(InternalEntry entry) {
                    InternalKey k = entry.getKey();
                    int curT = Math.max(k.getStartTime().valInt(), tx.getDepartureTime());
                    int travelT = entry.getValue().getInt(0);
                    if(curT +travelT<minArriveT) minArriveT = curT +travelT;
                }
                @Override
                public Object onReturn() {
                    if(minArriveT<Integer.MAX_VALUE){
                        return minArriveT;
                    }else{
                        return -1;
                    }
                }
            });
            if (tObj == null) {
                result.setArriveTime(-1);
            }else{
                result.setArriveTime((Integer) tObj);
            }
        }
        return result;
    }

    private Result execute(ImportStaticDataTx tx){
        try(Transaction t = db.beginTx()) {
            for (ImportStaticDataTx.StaticCrossNode p : tx.getCrosses()) {
                Node n = db.createNode();
                n.setProperty("name", p.getName());
                Preconditions.checkArgument(n.getId()==p.getId(), "id not match!");
            }
            for (ImportStaticDataTx.StaticRoadRel sr : tx.getRoads()) {
                Node start = db.getNodeById(sr.getStartCrossId());
                Node end = db.getNodeById(sr.getEndCrossId());
                Relationship r = start.createRelationshipTo(end, RoadType.ROAD_TO);
                Preconditions.checkArgument(r.getId()==sr.getRoadId(), "id not match");
            }
            t.success();
        }
        return new Result();
    }


    private Result execute(ImportTemporalDataTx tx) {
        try(Transaction t = db.beginTx()) {
            for(ImportTemporalDataTx.StatusUpdate s : tx.data){
                Relationship r = db.getRelationshipById(s.getRoadId());
                TimePoint time = Helper.time(s.getTime());
                r.setTemporalProperty("travel_time", time, s.getTravelTime());
                r.setTemporalProperty("full_status", time, s.getJamStatus());
                r.setTemporalProperty("segment_count", time, s.getSegmentCount());
            }
            t.success();
        }
        return new Result();
    }

    private Result execute(ReachableAreaQueryTx tx){
        try(Transaction t = db.beginTx()) {
            EarliestArriveTime algo = new EarliestArriveTimeTGraphKernel(db, "travel_time", tx.getStartCrossId(), tx.getDepartureTime(), tx.getTravelTime());
            List<EarliestArriveTime.NodeCross> answers = new ArrayList<>(algo.run());
            answers.sort(Comparator.comparingLong(EarliestArriveTime.NodeCross::getId));
            ReachableAreaQueryTx.Result result = new ReachableAreaQueryTx.Result();
            result.setNodeArriveTime(answers);
//            t.failure();//do not commit;
            return result;
        }
    }

    private Result execute(NodeNeighborRoadTx tx){
        try(Transaction t = db.beginTx()) {
            List<Long> answers = new ArrayList<>();
            for(Relationship r : db.getNodeById(tx.getNodeId()).getRelationships(RoadType.ROAD_TO, Direction.OUTGOING)) {
                answers.add(r.getId());
            }
            answers.sort(Comparator.naturalOrder());
            NodeNeighborRoadTx.Result result = new NodeNeighborRoadTx.Result();
            result.setRoadIds(answers);
//            t.failure();//do not commit;
            return result;
        }
    }
}
