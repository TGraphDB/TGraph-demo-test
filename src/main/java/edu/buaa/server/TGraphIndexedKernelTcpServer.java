package edu.buaa.server;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import edu.buaa.algo.EarliestArriveTime;
import edu.buaa.benchmark.transaction.*;
import edu.buaa.benchmark.transaction.AbstractTransaction.Result;
import edu.buaa.benchmark.transaction.internal.CreateTGraphAggrDurationIndexTx;
import edu.buaa.benchmark.transaction.internal.CreateTGraphAggrMaxIndexTx;
import edu.buaa.benchmark.transaction.internal.CreateTGraphTemporalValueIndexTx;
import edu.buaa.benchmark.transaction.internal.EarliestArriveTimeAggrTx;
import edu.buaa.client.RuntimeEnv;
import edu.buaa.model.StatusUpdate;
import edu.buaa.utils.Helper;
import edu.buaa.utils.Pair;
import edu.buaa.utils.TGraphSocketServer;
import edu.buaa.utils.Triple;
import org.act.temporalProperty.index.IndexType;
import org.act.temporalProperty.query.TimePointL;
import org.neo4j.graphdb.*;
import org.neo4j.temporal.TemporalRangeQuery;
import org.neo4j.temporal.TimePoint;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TGraphIndexedKernelTcpServer extends TGraphKernelTcpServer {
    public static void main(String[] args){
        TGraphSocketServer server = new TGraphSocketServer( dbDir(), new TGraphIndexedKernelTcpServer() );
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
                String roadName = (String) r.getProperty("name");
                Preconditions.checkNotNull(roadName,"should not happen: r name==null");
                roadMap.put(roadName, r.getId());
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
            case tx_query_snapshot_aggr_max: return execute((SnapshotAggrMaxTx) tx);
            case tx_query_snapshot_aggr_duration: return execute((SnapshotAggrDurationTx) tx);
            case tx_query_road_by_temporal_condition: return execute((EntityTemporalConditionTx) tx);
            case tx_index_tgraph_aggr_max: return execute((CreateTGraphAggrMaxIndexTx) tx);
            case tx_index_tgraph_aggr_duration: return execute((CreateTGraphAggrDurationIndexTx) tx);
            case tx_index_tgraph_temporal_condition: return execute((CreateTGraphTemporalValueIndexTx) tx);
            default:
                throw new UnsupportedOperationException("This class is used for testing index speed up only.");
        }
    }

    private Result execute(CreateTGraphTemporalValueIndexTx tx) {
        long indexId = -1;
        try(Transaction t = db.beginTx()){
            indexId = db.temporalIndex().relCreateValueIndex(Helper.time(tx.getStart()), Helper.time(tx.getEnd()), tx.getProName(), tx.getEvery(), tx.getTimeUnit(), IndexType.AGGR_MAX);
//            db.temporalIndex().awaitIndexOnline(indexId);
        }
        CreateTGraphAggrMaxIndexTx.Result r = new CreateTGraphAggrMaxIndexTx.Result();
        r.setIndexId(indexId);
        return r;
    }

    private Result execute(CreateTGraphAggrDurationIndexTx tx) {
        long indexId = -1;
        try(Transaction t = db.beginTx()){
            indexId = db.temporalIndex().relCreateDurationIndex(Helper.time(tx.getStart()), Helper.time(tx.getEnd()), tx.getProName(), tx.getEvery(), tx.getTimeUnit());
//            db.temporalIndex().awaitIndexOnline(indexId);
        }
        CreateTGraphAggrMaxIndexTx.Result r = new CreateTGraphAggrMaxIndexTx.Result();
        r.setIndexId(indexId);
        return r;
    }

    private Result execute(CreateTGraphAggrMaxIndexTx tx) {
        long indexId = -1;
        try(Transaction t = db.beginTx()){
            indexId = db.temporalIndex().relCreateMinMaxIndex(Helper.time(tx.getStart()), Helper.time(tx.getEnd()), tx.getProName(), tx.getEvery(), tx.getTimeUnit(), IndexType.AGGR_MAX);
            db.temporalIndex().awaitIndexOnline(indexId);
        }
        CreateTGraphAggrMaxIndexTx.Result r = new CreateTGraphAggrMaxIndexTx.Result();
        r.setIndexId(indexId);
        return r;
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

    private Result execute(SnapshotAggrMaxTx tx){
        try(Transaction t = db.beginTx()){
            List<Pair<String, Integer>> answers = new ArrayList<>();
            List<Integer> travelTime = new ArrayList<>();   //定义了一个存储最大值的列表
            for (Relationship r:GlobalGraphOperations.at(db).getAllRelationships()){
                String roadName = (String) r.getProperty("name");
                Object v = r.getTemporalPropertyWithIndex(tx.getP(), Helper.time(tx.getT0()), Helper.time(tx.getT1()), indexId);
            }
            SnapshotAggrMaxTx.Result result = new SnapshotAggrMaxTx.Result();
            result.setRoadTravelTime(answers);
            return result;
        }
    }

    private Result execute(SnapshotAggrDurationTx tx) {
        try (Transaction t = db.beginTx()) {
            List<Triple<String, Integer, Integer>> answers = new ArrayList<>();
            for (Relationship r : GlobalGraphOperations.at(db).getAllRelationships()) {
                String roadName = (String) r.getProperty("name");
                Object v = r.getTemporalPropertyWithIndex(tx.getP(), Helper.time(tx.getT0()), Helper.time(tx.getT1()), indexId);
            }
            SnapshotAggrDurationTx.Result result = new SnapshotAggrDurationTx.Result();
            result.setRoadStatDuration(answers);
            return result;
        }
    }

    private Result execute(EntityTemporalConditionTx tx){
        try(Transaction t = db.beginTx()){
            List<String> answers = new ArrayList<>();
            for (Relationship r:GlobalGraphOperations.at(db).getAllRelationships()){
                String roadName = (String) r.getProperty("travel_time");
                Object v = r.getTemporalProperty(tx.getP(), Helper.time(tx.getT0()), Helper.time(tx.getT1()), new TemporalRangeQuery() {
                    @Override
                    public void onNewEntry(long entityId, int propertyId, TimePointL time, Object val) {
                        if ((Integer)val > tx.getVmin()){
                            answers.add(tx.getP());
                        }
                    }
                    @Override
                    public Object onReturn() {
                        return null;
                    }
                });
            }
            List<String> answersFinal = answers.stream().distinct().collect(Collectors.toList());
            EntityTemporalConditionTx.Result result = new EntityTemporalConditionTx.Result();
            result.setRoads(answers);
            return result;
        }
    }

}
