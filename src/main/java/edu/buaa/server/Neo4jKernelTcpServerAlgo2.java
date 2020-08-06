package edu.buaa.server;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import edu.buaa.algo.EarliestArriveTime;
import edu.buaa.benchmark.transaction.*;
import edu.buaa.utils.TGraphSocketServer;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Neo4jKernelTcpServerAlgo2 {

    public static abstract class Neo4jBackEnd extends TGraphSocketServer.ReqExecutor {
        @Override
        protected AbstractTransaction.Result execute(String line) throws RuntimeException {
            AbstractTransaction tx = JSON.parseObject(line, AbstractTransaction.class);
            switch (tx.getTxType()){
                case tx_import_static_data: return execute((ImportStaticDataTx) tx);
                case tx_import_temporal_data: return execute((ImportTemporalDataTx) tx);
                case tx_query_reachable_area: return execute((ReachableAreaQueryTx) tx);
//                case tx_query_road_earliest_arrive_time_aggr: return execute((EarliestArriveTimeAggrTx)tx);
                case tx_query_node_neighbor_road: return execute((NodeNeighborRoadTx) tx);
                default:
                    throw new UnsupportedOperationException();
            }
        }

        private AbstractTransaction.Result execute(ImportStaticDataTx tx) {
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
            return new AbstractTransaction.Result();
        }

        private AbstractTransaction.Result execute(ImportTemporalDataTx tx) {
            return new AbstractTransaction.Result();
        }

        private AbstractTransaction.Result execute(UpdateTemporalDataTx tx) {
            return new AbstractTransaction.Result();
        }

        private AbstractTransaction.Result execute(SnapshotQueryTx tx) {
            return new AbstractTransaction.Result();
        }

        private AbstractTransaction.Result execute(SnapshotAggrMaxTx tx) {
            return new AbstractTransaction.Result();
        }

        private AbstractTransaction.Result execute(SnapshotAggrDurationTx tx) {
            return new AbstractTransaction.Result();
        }

        private AbstractTransaction.Result execute(EntityTemporalConditionTx tx) {
            return new AbstractTransaction.Result();
        }

        private AbstractTransaction.Result execute(NodeNeighborRoadTx tx){
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

        private AbstractTransaction.Result execute(ReachableAreaQueryTx tx) {
            try(Transaction t = db.beginTx()) {
                return null;
            }

        }

    }

}
