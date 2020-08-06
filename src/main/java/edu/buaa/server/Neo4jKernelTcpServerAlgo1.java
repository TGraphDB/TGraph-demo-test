package edu.buaa.server;

import com.alibaba.fastjson.JSON;
import edu.buaa.algo.EarliestArriveTime;
import edu.buaa.benchmark.transaction.*;
import edu.buaa.model.StatusUpdate;
import edu.buaa.utils.TGraphSocketServer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import scala.Tuple4;

import java.io.File;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Neo4jKernelTcpServerAlgo1 {

    private GraphDatabaseService db;

    public void createDB() {
        String databasePathName = "";
        File file = new File(databasePathName);
        db = new GraphDatabaseFactory().newEmbeddedDatabase(file);
    }

    private static void registerShutdownHook(final GraphDatabaseService db) {
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> db.shutdown())
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
//                case tx_query_road_earliest_arrive_time_aggr: return execute((EarliestArriveTimeAggrTx)tx);
                case tx_query_node_neighbor_road: return execute((NodeNeighborRoadTx) tx);
                default:
                    throw new UnsupportedOperationException();
            }
        }


        private AbstractTransaction.Result execute(ImportStaticDataTx tx) {
            try(Transaction t = db.beginTx()) {
                for (ImportStaticDataTx.StaticRoadRel road : tx.getRoads()) {
                    Node node = db.createNode();
                    Label roadLabel = () -> "Road";
                    node.addLabel(roadLabel);
//                    node.addLabel(Label.label("Road"));
                    node.setProperty("name", road.getId());
                    node.setProperty("road_id", road.getRoadId());
                    node.setProperty("start_cross_id", road.getStartCrossId());
                    node.setProperty("end_cross_id", road.getEndCrossId());
                    node.setProperty("length", road.getLength());
                    node.setProperty("type", road.getType());
                }

                for (ImportStaticDataTx.StaticCrossNode cross : tx.getCrosses()) {
                    long id = cross.getId();
                    Label roadLabel = () -> "Road";
                    Label crossLabel = () -> "Cross";
                    // build relationship between a and b when a is able to reach b
                    ResourceIterator<Node> endNodes = db.findNodes(roadLabel, "end_cross_id", id);
                    ResourceIterator<Node> startNodes = db.findNodes(roadLabel, "start_cross_id", id);
                    for (ResourceIterator<Node> i = endNodes; i.hasNext(); ) {
                        Node endNode = i.next();
                        for (ResourceIterator<Node> j = startNodes; j.hasNext(); ) {
                            Node startNode = j.next();
                            endNode.createRelationshipTo(startNode, Edge.REACH_TO);
                            if ((long)endNode.getProperty("start_cross_id") == (long)startNode.getProperty("end_cross_id")) {
                                startNode.createRelationshipTo(endNode, Edge.REACH_TO);
                            }
                        }
                    }
                    // build cross nodes
                    Node crossNode = db.createNode(crossLabel);
                    crossNode.setProperty("id", cross.getId());
                    crossNode.setProperty("name", cross.getName());
                    for (ResourceIterator<Node> it = startNodes; it.hasNext(); ) {
                        crossNode.createRelationshipTo(it.next(), Edge.IS_START_CROSS);
                    }
                    for (ResourceIterator<Node> it = endNodes; it.hasNext(); ) {
                        crossNode.createRelationshipTo(it.next(), Edge.IS_END_CROSS);
                    }
                }
                t.success();
            }
            return new AbstractTransaction.Result();
        }

        private AbstractTransaction.Result execute(ImportTemporalDataTx tx) {
            try (Transaction t = db.beginTx()) {
                Label timeLabel = () -> "Duration";
                ArrayList<TemporalData> dataList = getTemporalDataSerial((ArrayList<StatusUpdate>) tx.data);
                for (TemporalData temporalData : dataList) {
                    Node start = db.findNode(() -> "Road", "name", temporalData.roadName);
                    for (TemporalData.Data data : temporalData.changedData) {
                        Node end = db.createNode(timeLabel);
                        Relationship relationship = start.createRelationshipTo(end, Edge.TIME_FROM_START_TO_END);
                        relationship.setProperty("status", data.status);
                        relationship.setProperty("travel_time", data.travelTime);
                        relationship.setProperty("seg_cnt", data.segmentCount);
                        end.setProperty("start_time", data.startTime);
                        end.setProperty("end_time", data.endTime);
                    }
                }
                t.success();
            }
            return new AbstractTransaction.Result();
        }

        private static class TemporalData {
            private String roadName;
            private ArrayList<Data> changedData;

            public TemporalData(String roadName, ArrayList<Data> changedData) {
                this.roadName = roadName;
                this.changedData = changedData;
            }

            private static class Data {
                private int startTime;
                private int endTime;
                private int travelTime;
                private int status;
                private int segmentCount;
                public Data(int startTime, int endTime, int travelTime, int status, int segmentCount) {
                    this.startTime = startTime;
                    this.endTime = endTime;
                    this.travelTime = travelTime;
                    this.status = status;
                    this.segmentCount = segmentCount;
                }
            }
        }

        // todo

        private ArrayList<TemporalData> getTemporalDataSerial(ArrayList<StatusUpdate> dataList) {
            ArrayList<TemporalData> ret = new ArrayList<>();
            HashMap<String, ArrayList<TemporalData.Data>> tmp = new HashMap<>();
            for (StatusUpdate s : dataList) {
                int inf = Integer.MAX_VALUE;
                int updateTime = s.getTime();
                int travelTime = s.getTravelTime();
                int segCnt = s.getSegmentCount();
                int status = s.getJamStatus();
                String address = s.getRoadId();
                ArrayList<TemporalData.Data> value = tmp.get(address);
                if (value != null) {
                    value.add(new TemporalData.Data(updateTime, inf, travelTime, status, segCnt));
                } else {
                    value = new ArrayList<>();
                    value.add(new TemporalData.Data(updateTime, inf, travelTime, status, segCnt));
                    tmp.put(address, value);
                }
            }
            tmp.forEach((k, v) -> {
                Collections.sort(v, Comparator.comparingInt(o -> o.startTime));
                for (int i = 1; i < v.size(); ++i) {
                    TemporalData.Data newValue = v.get(i - 1);
                    newValue.endTime = v.get(i).startTime - 1;
                    v.set(i-1, newValue);
                }
                ret.add(new TemporalData(k, v));
            });
            return ret;
        }


        private AbstractTransaction.Result execute(UpdateTemporalDataTx tx) {
            try (Transaction t = db.beginTx()) {
                int segCnt = tx.getSegmentCount();
                int startTime = tx.getStartTime();
                int endTime = tx.getEndTime();
                int status = tx.getJamStatus();
                int travelTime = tx.getTravelTime();
                long rId = tx.getRoadId();
                Node start = db.findNode(() -> "Road", "road_id", rId);
                Iterable<Relationship> relationships = start.getRelationships(Edge.TIME_FROM_START_TO_END, Direction.OUTGOING);
                HashMap<Node, Relationship> ends = new HashMap<>();
                for (Relationship relationship : relationships) {
                    ends.put(relationship.getEndNode(), relationship);
                }
                AtomicBoolean hasInner = new AtomicBoolean(false);
                ends.forEach((node, relationship) -> {
                    if (hasInner.get()) return ;
                    int oldSegCnt = (int)relationship.getProperty("seg_cnt");
                    int oldTravelTime = (int)relationship.getProperty("travel_time");
                    int oldStatus = (int)relationship.getProperty("status");
                    int st = (Integer)node.getProperty("start_time");
                    int en = (Integer)node.getProperty("end_time");
                    if (endTime >= st && startTime < en) {
                        ends.get(node).delete();
                        node.delete();
                        if (startTime >= st && endTime <= en) { // inner
                            hasInner.set(true);
                            Node newLeftNode = db.createNode(() -> "Duration");
                            Node newMiddleNode = db.createNode(() -> "Duration");
                            Node newRightNode = db.createNode(() -> "Duration");
                            newLeftNode.setProperty("start_time", st);
                            newLeftNode.setProperty("end_time", startTime - 1);
                            Relationship newRelationship = start.createRelationshipTo(newLeftNode, Edge.TIME_FROM_START_TO_END);
                            newRelationship.setProperty("status", oldStatus);
                            newRelationship.setProperty("travel_time", oldTravelTime);
                            newRelationship.setProperty("seg_cnt", oldSegCnt);
                            newMiddleNode.setProperty("start_time", startTime);
                            newMiddleNode.setProperty("end_time", endTime - 1);
                            newRelationship = start.createRelationshipTo(newMiddleNode, Edge.TIME_FROM_START_TO_END);
                            newRelationship.setProperty("status", status);
                            newRelationship.setProperty("travel_time", travelTime);
                            newRelationship.setProperty("seg_cnt", segCnt);
                            newRightNode.setProperty("start_time", endTime - 1);
                            newRightNode.setProperty("end_time", en);
                            newRelationship = start.createRelationshipTo(newRightNode, Edge.TIME_FROM_START_TO_END);
                            newRelationship.setProperty("status", oldStatus);
                            newRelationship.setProperty("travel_time", oldTravelTime);
                            newRelationship.setProperty("seg_cnt", oldSegCnt);
                            return ;
                        }
                        if (startTime <= st && endTime >= en) { // middle
                            // nop
                        } else if (startTime >= st) { // left
                            Node newNode = db.createNode(() -> "Duration");
                            newNode.setProperty("start_time", st);
                            newNode.setProperty("end_time", startTime - 1);
                            Relationship newRelationship = start.createRelationshipTo(newNode, Edge.TIME_FROM_START_TO_END);
                            newRelationship.setProperty("status", oldStatus);
                            newRelationship.setProperty("travel_time", oldTravelTime);
                            newRelationship.setProperty("seg_cnt", oldSegCnt);
                        } else if (endTime <= en) { // right
                            Node newNode = db.createNode(() -> "Duration");
                            newNode.setProperty("start_time", endTime);
                            newNode.setProperty("end_time", en);
                            Relationship newRelationship = start.createRelationshipTo(newNode, Edge.TIME_FROM_START_TO_END);
                            newRelationship.setProperty("status", oldStatus);
                            newRelationship.setProperty("travel_time", oldTravelTime);
                            newRelationship.setProperty("seg_cnt", oldSegCnt);
                        }
                    }
                });
                if (!hasInner.get()) {
                    Node newNode = db.createNode(() -> "Duration");
                    newNode.setProperty("start_time", startTime);
                    newNode.setProperty("end_time", endTime - 1);
                    Relationship newRelationship = start.createRelationshipTo(newNode, Edge.TIME_FROM_START_TO_END);
                    newRelationship.setProperty("status", status);
                    newRelationship.setProperty("travel_time", travelTime);
                    newRelationship.setProperty("seg_cnt", segCnt);
                }
                t.success();
            }
            return new AbstractTransaction.Result();
        }

        private AbstractTransaction.Result execute(SnapshotQueryTx tx) {
            try (Transaction t = db.beginTx()) {
                List<Pair<Long, Integer>> res = new ArrayList<>();
                int time = tx.getTimestamp();
                ResourceIterator<Node> nodes = db.findNodes(() -> "Road");
                for (ResourceIterator<Node> it = nodes; it.hasNext(); ) {
                    Node node = it.next();
                    Iterable<Relationship> rel = node.getRelationships(Edge.TIME_FROM_START_TO_END, Direction.OUTGOING);
                    for (Relationship r : rel) {
                        Node end = r.getEndNode();
                        int startTime = (int)end.getProperty("start_time");
                        int endTime = (int)end.getProperty("end_time");
                        if (time >= startTime && time <= endTime) {
                            res.add(Pair.of((long)node.getProperty("road_id"), (int)r.getProperty("travel_time")));
                            break;
                        }
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
                List<Pair<Long, Integer>> res = new ArrayList<>();
                int st = tx.getT0();
                int en = tx.getT1();
                ResourceIterator<Node> nodes = db.findNodes(() -> "Road");
                for (ResourceIterator<Node> it = nodes; it.hasNext(); ) {
                    Node node = it.next();
                    Iterable<Relationship> rel = node.getRelationships(Edge.TIME_FROM_START_TO_END, Direction.OUTGOING);
                    ArrayList<Pair<Long, Integer>> tmp = new ArrayList<>();
                    for (Relationship r : rel) {
                        Node end = r.getEndNode();
                        int startTime = (int)end.getProperty("start_time");
                        int endTime = (int)end.getProperty("end_time");
                        if (en >= startTime && st <= endTime) {
                            tmp.add(Pair.of((long)node.getProperty("road_id"), (int)r.getProperty("travel_time")));
                        }
                    }
                    res.add(Collections.max(tmp, (o1, o2) -> {
                        return (o2.getRight() - o1.getRight());
                    }));
                }
                t.success();
                SnapshotAggrMaxTx.Result result = new SnapshotAggrMaxTx.Result();
                result.setRoadTravelTime(res);
                return result;
            }
        }

        // todo 如何在(t0, t1)间状态值相同， 求和
        private AbstractTransaction.Result execute(SnapshotAggrDurationTx tx) {
            try (Transaction t = db.beginTx()) {
                List<Triple<Long, Integer, Integer>> res = new ArrayList<>();
                int t0 = tx.getT0();
                int t1 = tx.getT1();
                ResourceIterator<Node> nodes = db.findNodes(() -> "Road");
                for (ResourceIterator<Node> it = nodes; it.hasNext(); ) {
                    Node node = it.next();
                    Iterable<Relationship> rel = node.getRelationships(Edge.TIME_FROM_START_TO_END, Direction.OUTGOING);
                    for (Relationship r : rel) {
                        Node end = r.getEndNode();
                        int startTime = (int)end.getProperty("start_time");
                        int endTime = (int)end.getProperty("end_time");
                        if (t1 >= startTime && t0 <= endTime) {
                            if (t0 >= startTime && t1 <= endTime) { // inner
                                int duration = endTime - startTime + 1;
                                res.add(Triple.of((long)node.getProperty("road_id"), (int)r.getProperty("travel_time"), duration));
                                continue;
                            }
                            if (t0 <= startTime && t1 >= endTime) { // middle
                                int duration = t1 - t0 + 1;
                                res.add(Triple.of((long)node.getProperty("road_id"), (int)r.getProperty("travel_time"), duration));
                            } else if (t0 >= startTime) { // left
                                int duration = endTime - t0 + 1;
                                res.add(Triple.of((long)node.getProperty("road_id"), (int)r.getProperty("travel_time"), duration));
                            } else if (t1 <= endTime) { // right
                                int duration = t1 - startTime + 1;
                                res.add(Triple.of((long)node.getProperty("road_id"), (int)r.getProperty("travel_time"), duration));
                            }
                        }
                    }
                }
                t.success();
                SnapshotAggrDurationTx.Result result = new SnapshotAggrDurationTx.Result();
                result.setRoadStatDuration(res);
                return result;
            }
        }

        private AbstractTransaction.Result execute(EntityTemporalConditionTx tx) {
            try (Transaction t = db.beginTx()) {
                int t0 = tx.getT0();
                int t1 = tx.getT1();
                int vMin = tx.getVmin();
                int vMax = tx.getVmax();
                ArrayList<Long> res = new ArrayList<>();
                ResourceIterator<Node> nodes = db.findNodes(() -> "Road");
                for (ResourceIterator<Node> it = nodes; it.hasNext(); ) {
                    Node node = it.next();
                    Iterable<Relationship> rel = node.getRelationships(Edge.TIME_FROM_START_TO_END, Direction.OUTGOING);
                    for (Relationship r : rel) {
                        Node end = r.getEndNode();
                        int startTime = (int)end.getProperty("start_time");
                        int endTime = (int)end.getProperty("end_time");
                        int travelTime = (int)r.getProperty("travel_t");
                        if (t1 >= startTime && t0 <= endTime && travelTime >= vMin && travelTime <= vMax) {
                            res.add((Long)node.getProperty("road_id"));
                        }
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
                long id = tx.getNodeId();
                Node cross = db.findNode(() -> "Cross", "id", id);
                Iterable<Relationship> start = cross.getRelationships(Edge.IS_START_CROSS, Direction.OUTGOING);
                Iterable<Relationship> end = cross.getRelationships(Edge.IS_END_CROSS, Direction.OUTGOING);
                List<Long> res = new ArrayList<>();
                for (Relationship rel : start) {
                    res.add((long)rel.getEndNode().getProperty("road_id"));
                }
                for (Relationship rel : end) {
                    res.add((long)rel.getEndNode().getProperty("road_id"));
                }
                NodeNeighborRoadTx.Result result = new NodeNeighborRoadTx.Result();
                result.setRoadIds(res);
                t.success();
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
                List<Tuple4<Long, Long, Long, Integer>> currentPath = new ArrayList<>();
                HashMap<Long, Integer> dis = new HashMap<>();
                que.add(Tuple4.apply(outset, 0, givenTime, currentTime));
                while (!que.isEmpty()) {
                    currentPath.clear();
                    Tuple4<Long, Integer, Integer, Integer> top = que.poll();
                    long curCrossId = top._1();
                    int cost = top._2();
                    int remainTime = top._3();
                    currentTime = top._4();
                    Node node = db.findNode(() -> "Cross", "id", curCrossId);
                    Iterable<Relationship> rel = node.getRelationships(Edge.IS_START_CROSS, Direction.OUTGOING);
                    for (Relationship r : rel) {
                        Node path = r.getEndNode();
                        int travelTime = leastTimeFromStartToEnd(path, currentTime, currentTime + remainTime);
                        if (remainTime >= travelTime) {
                            long rId = (long)path.getProperty("road_id");
                            long endCrossId = (long)path.getProperty("end_cross_id");
                            currentPath.add(Tuple4.apply(rId, curCrossId, endCrossId, travelTime));
                        }
                    }
                    for (Tuple4<Long, Long, Long, Integer> item : currentPath) {
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
        private static int leastTimeFromStartToEnd(Node path, int dePartureTime, int endTime) {
            Iterable<Relationship> rel = path.getRelationships(Edge.TIME_FROM_START_TO_END, Direction.OUTGOING);
            ArrayList<Node> timeNode = new ArrayList<>();
            for (Relationship r : rel) {
                Node end = r.getEndNode();
//                int st = (int)end.getProperty("start_time");
//                int en = (int)end.getProperty("end_time");
                timeNode.add(end);
            }
            Collections.sort(timeNode, (o1, o2) -> (int)o1.getProperty("start_time") - (int)o2.getProperty("end_time"));
            int ret = Integer.MAX_VALUE;
            for (Node node : timeNode) {
                int st = (int)node.getProperty("start_time");
                int en = (int)node.getProperty("end_time");
                Relationship r = node.getSingleRelationship(Edge.TIME_FROM_START_TO_END, Direction.INCOMING);
                int travelTime = (int)r.getProperty("travel_t");
                if (dePartureTime > en || endTime < st) continue;
                if (dePartureTime >= st && endTime <= en) { // inner
                    if (dePartureTime + travelTime > Math.max(en, endTime)) continue;
                    if (ret > travelTime) ret = travelTime;
                    continue;
                }
                if (dePartureTime >= st) { // left
                    if (dePartureTime + travelTime > Math.max(en, endTime)) continue;
                    if (ret > travelTime) ret = travelTime;
                } else if (dePartureTime <= st && endTime >= en) { // middle
                    if (st + travelTime > Math.max(en, endTime)) continue;
                    if (ret > travelTime + st - dePartureTime) ret = travelTime + st - dePartureTime;
                } else if (endTime < en) { // right
                    if (st + travelTime > endTime) continue;
                    if (ret > travelTime + st - dePartureTime) ret = travelTime + st - dePartureTime;
                }
            }
            return ret;
        }

        private static enum Edge implements RelationshipType {
            TIME_FROM_START_TO_END, REACH_TO, IS_START_CROSS, IS_END_CROSS
        }

    }

}
