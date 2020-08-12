package edu.buaa.server;

import com.alibaba.fastjson.JSON;
import edu.buaa.algo.EarliestArriveTime;
import edu.buaa.benchmark.transaction.*;
import edu.buaa.benchmark.transaction.internal.NodeNeighborRoadTx;
import edu.buaa.model.StatusUpdate;
import edu.buaa.utils.TGraphSocketServer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.kernel.impl.coreapi.IndexManagerImpl;
import scala.Tuple4;

import java.io.File;
import java.util.*;

public class Neo4jKernelTcpServerAlgo2 {

    private GraphDatabaseService db;

    public void createDB() {
        String databasePathName = "";
        File file = new File(databasePathName);
        db = new GraphDatabaseFactory().newEmbeddedDatabase(file);
        try (Transaction tx = db.beginTx()) {
            Schema schema = db.schema();
            schema.indexFor(Label.label("Road")).on("name").create();
            schema.indexFor(Label.label("Cross")).on("id").create();
            tx.success();
        }
    }

    private static void registerShutdownHook(final GraphDatabaseService db) {
        Runtime.getRuntime().addShutdownHook(
                new Thread(db::shutdown)
        );
    }

    public void shutdownDB() {
        registerShutdownHook(db);
    }

    public static abstract class Neo4jBackEnd extends TGraphSocketServer.ReqExecutor {
        @Override
        protected AbstractTransaction.Result execute(String line) throws RuntimeException {
            AbstractTransaction tx = JSON.parseObject(line, AbstractTransaction.class);
            switch (tx.getTxType()){
                case tx_import_static_data: return execute((ImportStaticDataTx) tx);
                case tx_import_temporal_data: return execute((ImportTemporalDataTx) tx);
                case tx_query_reachable_area: return execute((ReachableAreaQueryTx) tx);
                case tx_query_node_neighbor_road: return execute((NodeNeighborRoadTx) tx);
                default:
                    throw new UnsupportedOperationException();
            }
        }


        private AbstractTransaction.Result execute(ImportStaticDataTx tx) {
            try(Transaction t = db.beginTx()) {
                for (ImportStaticDataTx.StaticRoadRel road : tx.getRoads()) {
                    Node node = db.createNode(() -> "Road");
                    node.setProperty("length", road.getLength()); // int
                    node.setProperty("angle", road.getAngle()); // int
                    node.setProperty("type", road.getType()); // int
                    node.setProperty("r_id", road.getRoadId()); // long
                    node.setProperty("name", road.getId()); // String
                    node.setProperty("start_cross_id", road.getStartCrossId()); // long
                    node.setProperty("end_cross_id", road.getEndCrossId()); // long
                }
                for (ImportStaticDataTx.StaticCrossNode cross : tx.getCrosses()) {
                    Node node = db.createNode(() -> "Cross");
                    node.setProperty("id", cross.getId()); // long
                    node.setProperty("name", cross.getName()); // String
                    node.setProperty("latest_relation_id", -1); // long
                }

                t.success();
            }
            return new AbstractTransaction.Result();
        }

        private AbstractTransaction.Result execute(ImportTemporalDataTx tx) {
            try (Transaction t = db.beginTx()) {
                for (StatusUpdate s : tx.data) {
                    Node road = db.findNode(() -> "Road", "name", s.getRoadId());
                    long start = (long) road.getProperty("start_cross_id");
                    long end = (long) road.getProperty("end_cross_id");
                    Node startCross = db.findNode(() -> "Cross", "id", start);
                    Node endCross = db.findNode(() -> "Cross", "id", end);
                    long tag = (long) startCross.getProperty("latest_relation_id");
                    Relationship rel = startCross.createRelationshipTo(endCross, Edge.REACH_TO);
                    startCross.setProperty("latest_relation_id", rel.getId());
                    int status = s.getJamStatus();
                    int segCnt = s.getSegmentCount();
                    int travelTime = s.getTravelTime();
                    rel.setProperty("r_id", road.getId()); // make relation -> road O(1)
                    rel.setProperty("status", status);
                    rel.setProperty("seg_cnt", segCnt);
                    rel.setProperty("travel_time", travelTime);
                    int time = s.getTime();
                    rel.setProperty("start_time", time);
                    Relationship lastRel = db.getRelationshipById(tag);
                    if (lastRel != null) {
                        lastRel.setProperty("end_time", time);
                    }
                    rel.setProperty("end_time", Integer.MAX_VALUE);
                }
                t.success();
            }
            return new AbstractTransaction.Result();
        }

        private AbstractTransaction.Result execute(UpdateTemporalDataTx tx) {
            try (Transaction t = db.beginTx()) {
                // Do not consider that the old value equals to the new value
                int st = tx.getStartTime();
                int en = tx.getEndTime();
                Node road = db.findNode(() -> "Road", "name", tx.getRoadId());
                Node startCross = db.findNode(() -> "Cross", "id", road.getProperty("start_cross_id"));
                Node endCross = db.findNode(() -> "Cross", "id", road.getProperty("end_cross_id"));
                Iterable<Relationship> rels = startCross.getRelationships(Edge.REACH_TO, Direction.OUTGOING);
                ArrayList<Relationship> relsStart2End = new ArrayList<>();
                for (Relationship rel : rels) {
                    if (endCross.getId() == rel.getEndNode().getId()) {
                        relsStart2End.add(rel);
                    }
                }
                boolean hasInner = false;
                for (Relationship rel : relsStart2End) {
                    int startTime = (int) rel.getProperty("start_time");
                    int endTime = (int) rel.getProperty("end_time");
                    int status = (int) rel.getProperty("status");
                    int segCnt = (int) rel.getProperty("seg_cnt");
                    int travelTime = (int) rel.getProperty("travel_time");
                    if (en >= startTime && st < endTime) {
                        rel.delete();
                        if (st >= startTime && en <= endTime) { // inner
                            hasInner = true;
                            // split into three parts : start ~ st, st ~ en, en ~ end
                            if (st > startTime) {
                                Relationship left = startCross.createRelationshipTo(endCross, Edge.REACH_TO);
                                left.setProperty("status", status);
                                left.setProperty("seg_cnt", segCnt);
                                left.setProperty("travel_time", travelTime);
                                left.setProperty("start_time", startTime);
                                left.setProperty("end_time", st);
                            }
                            if (en > st) {
                                Relationship middle = startCross.createRelationshipTo(endCross, Edge.REACH_TO);
                                middle.setProperty("status", tx.getJamStatus());
                                middle.setProperty("seg_cnt", tx.getSegmentCount());
                                middle.setProperty("travel_time", tx.getTravelTime());
                                middle.setProperty("start_time", st);
                                middle.setProperty("end_time", en);
                            }
                            if (endTime > en) {
                                Relationship right = startCross.createRelationshipTo(endCross, Edge.REACH_TO);
                                right.setProperty("status", status);
                                right.setProperty("seg_cnt", segCnt);
                                right.setProperty("travel_time", travelTime);
                                right.setProperty("start_time", en);
                                right.setProperty("end_time", endTime);
                            }
                            break;
                        }
                        if (st > startTime) { // left
                            Relationship left = startCross.createRelationshipTo(endCross, Edge.REACH_TO);
                            left.setProperty("status", status);
                            left.setProperty("seg_cnt", segCnt);
                            left.setProperty("travel_time", travelTime);
                            left.setProperty("start_time", startTime);
                            left.setProperty("end_time", st);
                        } else if (en < endTime) { // right
                            Relationship right = startCross.createRelationshipTo(endCross, Edge.REACH_TO);
                            right.setProperty("status", status);
                            right.setProperty("seg_cnt", segCnt);
                            right.setProperty("travel_time", travelTime);
                            right.setProperty("start_time", en);
                            right.setProperty("end_time", endTime);
                        } else { // middle
                            // nop
                        }
                    }
                }
                if (!hasInner) {
                    Relationship middle = startCross.createRelationshipTo(endCross, Edge.REACH_TO);
                    middle.setProperty("status", tx.getJamStatus());
                    middle.setProperty("seg_cnt", tx.getSegmentCount());
                    middle.setProperty("travel_time", tx.getTravelTime());
                    middle.setProperty("start_time", st);
                    middle.setProperty("end_time", en);
                }
                t.success();
            }
            return new AbstractTransaction.Result();
        }

        private AbstractTransaction.Result execute(SnapshotQueryTx tx) {
            try (Transaction t = db.beginTx()) {
                List<Pair<String, Integer>> res = new ArrayList<>();
                int time = tx.getTimestamp();
                for (Relationship rel : db.getAllRelationships()) {
                    int startTime = (int) rel.getProperty("start_time");
                    int endTime = (int) rel.getProperty("end_time");
                    if (time >= startTime && time < endTime) {
                        int v = (int) rel.getProperty("status");
                        long rId = (long) rel.getProperty("id");
                        String name = (String) db.getNodeById(rId).getProperty("name");
                        res.add(Pair.of(name, v));
                    }
                }
                t.success();
                SnapshotQueryTx.Result result = new SnapshotQueryTx.Result();
                result.setRoadStatus(res);
                return result;
            }
        }

        private AbstractTransaction.Result execute(SnapshotAggrMaxTx tx) {
            try (Transaction t = db.beginTx()) {
                List<Pair<String, Integer>> res = new ArrayList<>();
                HashMap<String, Integer> tmp = new HashMap<>();
                int st = tx.getT0();
                int en = tx.getT1();
                for (Relationship rel : db.getAllRelationships()) {
                    int startTime = (int) rel.getProperty("start_time");
                    int endTime = (int) rel.getProperty("end_time");
                    if (en >= startTime && st < endTime) {
                        long rId = (long) rel.getProperty("id");
                        String name = (String) db.getNodeById(rId).getProperty("name");
                        int newV = (int) rel.getProperty("travel_time");
                        Integer v = tmp.get(name);
                        if (v == null) {
                            tmp.put(name, newV);
                        } else if (newV > v) {
                            tmp.put(name, newV);
                        }
                    }
                }
                t.success();
                tmp.forEach((k, v) -> res.add(Pair.of(k ,v)));
                SnapshotAggrMaxTx.Result result = new SnapshotAggrMaxTx.Result();
                result.setRoadTravelTime(res);
                return result;
            }
        }

        private AbstractTransaction.Result execute(SnapshotAggrDurationTx tx) {
            try (Transaction t = db.beginTx()) {
                List<Triple<String, Integer, Integer>> res = new ArrayList<>();
                // id, status, value
                HashMap<String, HashMap<Integer, Integer>> buffer = new HashMap<>();
                int st = tx.getT0();
                int en = tx.getT1();
                for (Relationship rel : db.getAllRelationships()) {
                    int startTime = (int) rel.getProperty("start_time");
                    int endTime = (int) rel.getProperty("end_time");
                    if (en >= startTime && st < endTime) {
                        long rId = (long) rel.getProperty("id");
                        String name = (String) db.getNodeById(rId).getProperty("name");
                        int value = (int) rel.getProperty("status");
                        int duration;
                        if (st >= startTime && en < endTime) { // inner
                            duration = en - st + 1;
                        } else if (st > startTime) { // left
                            duration = endTime - st;
                        } else if (en < endTime) { // right
                            duration = en - startTime + 1;
                        } else { // middle
                            duration = endTime - startTime;
                        }
                        HashMap<Integer, Integer> tmp = buffer.get(name);
                        if (tmp != null) {
                            Integer d = tmp.get(value);
                            if (d != null) {
                                tmp.put(value, d + duration);
                            } else {
                                tmp.put(value, duration);
                            }
                        } else {
                            tmp = new HashMap<>();
                            tmp.put(value, duration);
                        }
                        buffer.put(name, tmp);
                    }
                }
                t.success();
                buffer.forEach((key, value) -> value.forEach((k, v) -> res.add(Triple.of(key, k, v))));
                SnapshotAggrDurationTx.Result result = new SnapshotAggrDurationTx.Result();
                result.setRoadStatDuration(res);
                return result;
            }
        }

        private AbstractTransaction.Result execute(EntityTemporalConditionTx tx) {
            try (Transaction t = db.beginTx()) {
                int st = tx.getT0();
                int en = tx.getT1();
                int vMin = tx.getVmin();
                int vMax = tx.getVmax();
                ArrayList<String> res = new ArrayList<>();
                for (Relationship rel : db.getAllRelationships()) {
                    int startTime = (int) rel.getProperty("start_time");
                    int endTime = (int) rel.getProperty("end_time");
                    int v = (int) rel.getProperty("travel_time");
                    if (en >= startTime && st < endTime && v >= vMin && v <= vMax) {
                        long rId = (long) rel.getProperty("r_id");
                        res.add((String) db.getNodeById(rId).getProperty("name"));
                    }
                }
                t.success();
                EntityTemporalConditionTx.Result result = new EntityTemporalConditionTx.Result();
                result.setRoads(res);
                return result;
            }
        }

        private AbstractTransaction.Result execute(NodeNeighborRoadTx tx){
            try(Transaction t = db.beginTx()) {
                long crossId = tx.getNodeId();
                List<String> res = new ArrayList<>();
                ResourceIterator<Node> roads = db.findNodes(Label.label("Road"));
                for (ResourceIterator<Node> it = roads; it.hasNext(); ) {
                    Node road = it.next();
                    if ((int) road.getProperty("start_cross_id") == crossId || (int) road.getProperty("end_cross_id") == crossId) {
                        res.add((String) road.getProperty("name"));
                    }
                }
                t.success();
                NodeNeighborRoadTx.Result result = new NodeNeighborRoadTx.Result();
                result.setRoadIds(res);
                return result;
            }
        }

        private AbstractTransaction.Result execute(ReachableAreaQueryTx tx) {
            try(Transaction t = db.beginTx()) {
                long outset = tx.getStartCrossId();
                int currentTime = tx.getDepartureTime();
                int givenTime = tx.getTravelTime();
                //cross_id, cost_time, remain_time, cur_time
                PriorityQueue<Tuple4<Long, Integer, Integer, Integer>> que = new PriorityQueue<>(Comparator.comparingInt(Tuple4::_2));
                // r_id, r_start, r_end, travel_t_at_the_current_time
                List<Tuple4<String, Long, Long, Integer>> currentPath = new ArrayList<>();
                HashMap<Long, Integer> dis = new HashMap<>();
                que.add(Tuple4.apply(outset, 0, givenTime, currentTime));
                while (!que.isEmpty()) {
                    currentPath.clear();
                    Tuple4<Long, Integer, Integer, Integer> top = que.poll();
                    long curCrossId = top._1();
                    int cost = top._2();
                    int remainTime = top._3();
                    currentTime = top._4();
                    ArrayList<Node> roads = getAllPathOut(curCrossId, db.findNodes(Label.label("Road")));
                    for (Node road : roads) {
                        int travelTime = leastTimeFromStartToEnd(road, currentTime, currentTime + remainTime, db);
                        if (remainTime >= travelTime) {
                            String rId = (String) road.getProperty("name");
                            long endCrossId = (long) road.getProperty("end_cross_id");
                            currentPath.add(Tuple4.apply(rId, curCrossId, endCrossId, travelTime));
                        }
                    }
                    for (Tuple4<String, Long, Long, Integer> item : currentPath) {
                        if ((dis.get(item._3()) == null) || dis.get(item._3()) > cost + item._4()) {
                            dis.put(item._3(), cost + item._4());
                            que.add(Tuple4.apply(item._3(), cost + item._4(), remainTime - item._4(), currentTime + item._4()));
                        }
                    }

                }
                t.success();
                ReachableAreaQueryTx.Result result = new ReachableAreaQueryTx.Result();
                List<EarliestArriveTime.NodeCross> res = new ArrayList<>();
                dis.forEach((k, v) -> res.add(new EarliestArriveTime.NodeCross(k, v)));
                result.setNodeArriveTime(res);
                return result;
            }
        }

        private static ArrayList<Node> getAllPathOut(long crossId, ResourceIterator<Node> roads) {
            ArrayList<Node> res = new ArrayList<>();
            for (ResourceIterator<Node> it = roads; it.hasNext(); ) {
                Node road = it.next();
                if ((int) road.getProperty("start_cross_id") == crossId) {
                    res.add(road);
                }
            }
            return res;
        }

        private static int leastTimeFromStartToEnd(Node road, int dePartureTime, int endTime, GraphDatabaseService db) {
            long startCrossId = (long) road.getProperty("start_cross_id");
            long endCrossId = (long) road.getProperty("end_cross_id");
            Node startCross = db.findNode(Label.label("Cross"), "id", startCrossId);
            Iterable<Relationship> rels = startCross.getRelationships(Edge.REACH_TO, Direction.OUTGOING);
            ArrayList<Relationship> paths = new ArrayList<>();
            for (Relationship rel : rels) {
                if ((long) rel.getEndNode().getProperty("id") == endCrossId) {
                    paths.add(rel);
                }
            }
            int ret = Integer.MAX_VALUE;
            for (Relationship path : paths) {
                int st = (int) path.getProperty("start_time");
                int en = (int) path.getProperty("end_time");
                int travelTime = (int) path.getProperty("travel_time");
                if (dePartureTime >= en || endTime < st) continue;
                if (dePartureTime >= st && endTime < en) { // inner
                    if (dePartureTime + travelTime > Math.max(en - 1, endTime)) continue;
                    if (ret > travelTime) ret = travelTime;
                    continue;
                }
                if (dePartureTime >= st) { // left
                    if (dePartureTime + travelTime > Math.max(en - 1, endTime)) continue;
                    if (ret > travelTime) ret = travelTime;
                } else if (dePartureTime <= st && endTime >= en) { // middle
                    if (st + travelTime > Math.max(en - 1, endTime)) continue;
                    if (ret > travelTime + st - dePartureTime) ret = travelTime + st - dePartureTime;
                } else if (endTime < en) { // right
                    if (st + travelTime > endTime) continue;
                    if (ret > travelTime + st - dePartureTime) ret = travelTime + st - dePartureTime;
                }
            }
            return ret;
        }
        private enum Edge implements RelationshipType {
            REACH_TO
        }

    }

}
