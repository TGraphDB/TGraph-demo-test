package edu.buaa.server;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import edu.buaa.algo.EarliestArriveTime;
import edu.buaa.benchmark.transaction.*;
import edu.buaa.benchmark.transaction.AbstractTransaction.Result;
import edu.buaa.client.vo.RuntimeEnv;
import edu.buaa.server.neo4j.ArraySimulation;
import edu.buaa.utils.Helper;
import edu.buaa.utils.TGraphSocketServer;
import org.neo4j.graphdb.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Neo4jKernelTcpServer{
    public static void main(String[] args){
        String dbType = Helper.mustEnv("DB_TYPE");
        Neo4jBackEnd dbProxy;
        if("array".equals(dbType)){
            dbProxy = new ArraySimulation();
        }else{
            dbProxy = new TreeMapSimulation();
        }
        TGraphSocketServer server = new TGraphSocketServer( dbDir(), dbProxy );
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

    public static abstract class Neo4jBackEnd extends TGraphSocketServer.ReqExecutor {
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

        private Result execute(ImportStaticDataTx tx) {
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

        private Result execute(ReachableAreaQueryTx tx){
            try(Transaction t = db.beginTx()) {
                EarliestArriveTime algo = new EarliestArriveTimeNeo4jKernel(db, tx.getStartCrossId(), tx.getDepartureTime(), tx.getTravelTime()) {
                    @Override
                    protected int getEarliestArriveTime(Long roadId, int departureTime) throws UnsupportedOperationException {
                        return roadEAT(roadId, departureTime, this.endTime);
                    }
                };
                List<EarliestArriveTime.NodeCross> answers = new ArrayList<>(algo.run());
                answers.sort(Comparator.comparingLong(EarliestArriveTime.NodeCross::getId));
                ReachableAreaQueryTx.Result result = new ReachableAreaQueryTx.Result();
                result.setNodeArriveTime(answers);
//            t.failure();//do not commit;
                return result;
            }
        }

        private Result execute(EarliestArriveTimeAggrTx tx){
            EarliestArriveTimeAggrTx.Result result = new EarliestArriveTimeAggrTx.Result();
            try{
                result.setArriveTime(roadEAT(tx.getRoadId(), tx.getDepartureTime(), tx.getEndTime()));
            }catch (UnsupportedOperationException e){
                result.setArriveTime(-1);
            }
            return result;
        }

        protected abstract int roadEAT(long roadId, int departureTime, int endTime) throws UnsupportedOperationException;
        protected abstract Result execute(ImportTemporalDataTx tx);

    }

    private static abstract class EarliestArriveTimeNeo4jKernel extends EarliestArriveTime {

        private final GraphDatabaseService db;

        public EarliestArriveTimeNeo4jKernel(GraphDatabaseService db, long startId, int startTime, int travelTime){
            super(startId, startTime, travelTime);
            this.db = db;
        }

        protected abstract int getEarliestArriveTime(Long roadId, int departureTime) throws UnsupportedOperationException;

        @Override
        protected final Iterable<Long> getAllOutRoads(long nodeId) {
            Node node = db.getNodeById(nodeId);
            List<Long> result = new ArrayList<>();
            for(Relationship r : node.getRelationships(Direction.OUTGOING)){
                result.add(r.getId());
            }
            result.sort(Comparator.naturalOrder());
            return result;
        }

        @Override
        protected final long getEndNodeId(long roadId) {
            return db.getRelationshipById(roadId).getEndNode().getId();
        }
    }
}
