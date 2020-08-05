package edu.buaa.server;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import edu.buaa.algo.EarliestArriveTime;
import edu.buaa.benchmark.transaction.*;
import edu.buaa.benchmark.transaction.AbstractTransaction.Result;
import edu.buaa.client.RuntimeEnv;
import edu.buaa.model.StatusUpdate;
import edu.buaa.utils.Helper;
import edu.buaa.utils.TGraphSocketServer;
import org.act.temporalProperty.query.TimePointL;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.*;
import org.neo4j.temporal.TemporalRangeQuery;
import org.neo4j.temporal.TimePoint;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TGraphKernelTcpServer extends TGraphSocketServer.ReqExecutor {
    public static void main(String[] args){
        TGraphSocketServer server = new TGraphSocketServer( dbDir(), new TGraphKernelTcpServer() );
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

    private Map<String, Long> roadMap = new HashMap<>();

    @Override
    protected void setDB(GraphDatabaseService db){
        this.db = db;
        try(Transaction tx = db.beginTx()){
            for(Relationship r: GlobalGraphOperations.at(db).getAllRelationships()){
                roadMap.put((String) r.getProperty("name"), r.getId());
            }
            tx.success();
        }
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
            case tx_query_snapshot: return execute((SnapshotQueryTx) tx);
            default:
                throw new UnsupportedOperationException();
        }
    }

    private Result execute(EarliestArriveTimeAggrTx tx) {
        EarliestArriveTimeAggrTx.Result result = new EarliestArriveTimeAggrTx.Result();
        try(Transaction t = db.beginTx()) {
            Relationship r = db.getRelationshipById(tx.getRoadId());
            if (!r.hasProperty("travel_time")) throw new UnsupportedOperationException();
            Object tObj = r.getTemporalProperty("travel_time", Helper.time(tx.getDepartureTime()), Helper.time(tx.getEndTime()), new TemporalRangeQuery() {
                private int minArriveT = Integer.MAX_VALUE;
                private int entryIndex = 0;
                @Override
                public void onNewEntry(long entityId, int propertyId, TimePointL time, Object val) {
                    Preconditions.checkState(time.valInt() >= tx.getDepartureTime());
                    Preconditions.checkNotNull(val);
                    int curT = time.valInt();
                    if(entryIndex==0 && curT>tx.getDepartureTime()){
                        throw new UnsupportedOperationException();
                    }
                    entryIndex++;
                    int travelT = (int) val;
                    if(curT +travelT<minArriveT) minArriveT = curT +travelT;
                }
                @Override
                public Object onReturn() {
                    if(minArriveT<Integer.MAX_VALUE){
                        return minArriveT;
                    }else{
                        throw new UnsupportedOperationException();
                    }
                }
            });
            result.setArriveTime((Integer) tObj);
        }catch (UnsupportedOperationException e){
            result.setArriveTime(-1);
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
                r.setProperty("name", sr.getId());
                Preconditions.checkArgument(r.getId()==sr.getRoadId(), "id not match");
                roadMap.put(sr.getId(), r.getId());
            }
            t.success();
        }
        return new Result();
    }


    private Result execute(ImportTemporalDataTx tx) {
        try(Transaction t = db.beginTx()) {
            for(StatusUpdate s : tx.data){
                Relationship r = db.getRelationshipById(roadMap.get(s.getRoadId()));
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

    private Result execute(SnapshotQueryTx tx){
        try(Transaction t = db.beginTx()) {
            List<Pair<Long, Integer>> answers = new ArrayList<>();
            for(Relationship r: GlobalGraphOperations.at(db).getAllRelationships()){
                Object v = r.getTemporalProperty(tx.getPropertyName(), Helper.time(tx.getTimestamp()));
                if(v==null){
                    answers.add(Pair.of(r.getId(), -1));
                }else{
                    answers.add(Pair.of(r.getId(), (Integer) v));
                }
            }
            SnapshotQueryTx.Result result = new SnapshotQueryTx.Result();
//            answers.sort((pair)->{});
//            t.failure();//do not commit;
            result.setRoadStatus(answers);
            return result;
        }
    }

    public static class EarliestArriveTimeTGraphKernel extends EarliestArriveTime {
        private final String travelTimePropertyKey;
        private final GraphDatabaseService db;

        public EarliestArriveTimeTGraphKernel(GraphDatabaseService db, String travelTimePropertyKey, long startId, int startTime, int travelTime){
            super(startId, startTime, travelTime);
            this.db = db;
            this.travelTimePropertyKey = travelTimePropertyKey;
        }

        /**
         * TODO: this should be rewrite with an range query.
         * Use 'earliest arrive time' rather than simply use 'travel time' property at departureTime
         * Because there exist cases that 'a delay before departureTime decrease the time of
         * arrival'.(eg. wait until road not jammed, See Dreyfus 1969, page 29)
         * This makes the arrive-time-function non-decreasing, thus guarantee FIFO property of this temporal network.
         * This property is the foundational assumption to found earliest arrive time with this algorithm.
         * @param departureTime time start from r's start node.
         * @return earliest arrive time to r's end node when departure from r's start node at departureTime.
         */
        @Override
    //    protected int getEarliestArriveTime(Long roadId, int departureTime) throws UnsupportedOperationException {
    //        int minArriveTime = Integer.MAX_VALUE;
    //        Relationship r = db.getRelationshipById(roadId);
    //        if( !r.hasProperty( travelTimePropertyKey )) throw new UnsupportedOperationException();
    //        for(int curT = departureTime; curT<minArriveTime && curT<=endTime; curT++){
    //            Object tObj = r.getTemporalProperty( travelTimePropertyKey, Helper.time(curT));
    //            if(tObj==null) throw new UnsupportedOperationException();
    //            int period = (Integer) tObj;
    //            if (curT + period < minArriveTime) {
    //                minArriveTime = curT + period;
    //            }
    //        }
    //        return minArriveTime;
    //    }

        protected int getEarliestArriveTime(Long roadId, int departureTime) throws UnsupportedOperationException {
            Relationship r = db.getRelationshipById(roadId);
            if( !r.hasProperty( travelTimePropertyKey )) throw new UnsupportedOperationException();
            Object tObj = r.getTemporalProperty(travelTimePropertyKey, Helper.time(departureTime), Helper.time(this.endTime), new TemporalRangeQuery() {
                private int minArriveT = Integer.MAX_VALUE;
                private boolean firstEntry = true;
                @Override
                public void onNewEntry(long entityId, int propertyId, TimePointL time, Object val) {
                    Preconditions.checkState(time.valInt() >= departureTime);
                    Preconditions.checkNotNull(val);
                    int curT = time.valInt();
                    if(firstEntry && curT>departureTime){
                        throw new UnsupportedOperationException();
                    }
                    firstEntry=false;
                    int travelT = (int) val;
                    if(curT +travelT<minArriveT) minArriveT = curT +travelT;
                }
                @Override
                public Object onReturn() {
                    if(minArriveT<Integer.MAX_VALUE){
                        return minArriveT;
                    }else{
                        return null;
                    }
                }
            });
            if (tObj == null) {
                throw new UnsupportedOperationException();
            }else{
                return (Integer) tObj;
            }
        }

        @Override
        protected Iterable<Long> getAllOutRoads(long nodeId) {
            Node node = db.getNodeById(nodeId);
            List<Long> result = new ArrayList<>();
            for(Relationship r : node.getRelationships(Direction.OUTGOING)){
                result.add(r.getId());
            }
            result.sort(Comparator.naturalOrder());
            return result;
        }

        @Override
        protected long getEndNodeId(long roadId) {
            return db.getRelationshipById(roadId).getEndNode().getId();
        }
    }
}
