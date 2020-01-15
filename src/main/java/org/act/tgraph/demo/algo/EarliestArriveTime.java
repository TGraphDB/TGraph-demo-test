package org.act.tgraph.demo.algo;


import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.temporal.TimePoint;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class EarliestArriveTime {
    private final String EARLIEST_ARRIVE_TIME = "earliestArriveTime";
    private final String EARLIEST_ARRIVE_PATH_PARENT = "earliestArriveTime_parent";
    private final String EARLIEST_ARRIVE_NODE_STATUS = "earliestArriveTime_status";
//    private final String EARLIEST_ARRIVE_NODE_LEVEL = "earliestArriveTime_level";
    private final String travelTimePropertyKey;

    private final GraphDatabaseService db;
    private final long start;
    private final int startTime;
    private final int travelTime;

    public EarliestArriveTime(GraphDatabaseService db, String travelTimePropertyKey, long startId, int startTime, int travelTime){
        this.db = db;
        this.start = startId;
        this.startTime = startTime;
        this.travelTime = travelTime;
        this.travelTimePropertyKey = travelTimePropertyKey;
    }

    public List<Pair<Long, Integer>> runInTransaction() {
        for(Node node : GlobalGraphOperations.at(db).getAllNodes()){
            setStatus(node, Status.NotCalculate);
        }
        Node startNode = db.getNodeById(start);
        setStatus(startNode, Status.Calculating);
        setArriveTime(startNode, startTime);

        List<Pair<Long, Integer>> result = new ArrayList<>();
        Node node;
        while ((node = findSmallestClosedNode())!=null) {
            loopAllNeighborsUpdateArriveTime(node, result);
        }
        return result;
    }


    /**
     * loop through all neighbors of a given node, and for each neighbor node:
     * 1. update its earliest arrive time
     * 2. set parent to source node
     * 3. mark node status to CLOSE
     * after the loop above, mark given node status to FINISH
     */
    private void loopAllNeighborsUpdateArriveTime(Node node, List<Pair<Long, Integer>> result) {
        setStatus( node, Status.Calculated );
        int curTime = (Integer) node.getProperty( EARLIEST_ARRIVE_TIME );
        if( curTime > travelTime + startTime ) return;
        result.add( Pair.of( node.getId(), curTime ));
        for( Relationship r : node.getRelationships( Direction.OUTGOING )){
            Node neighbor = r.getEndNode();
            int arriveTime;
            try {
                switch (getStatus(neighbor)) {
                    case NotCalculate:
                        arriveTime = getEarliestArriveTime(r, curTime);
                        setArriveTime(neighbor, arriveTime);
                        setStatus(neighbor, Status.Calculating);
                        setParent(neighbor, node);
                        break;
                    case Calculating:
                        arriveTime = getEarliestArriveTime(r, curTime);
                        int curArriveTime = (Integer) neighbor.getProperty(EARLIEST_ARRIVE_TIME);
                        if (curArriveTime > arriveTime) {
                            setArriveTime(neighbor, arriveTime);
                            setParent(neighbor, node);
                        }
                        break;
                }
            }catch (UnsupportedOperationException ignore){}
        }
    }

    /**
     * TODO: this should be rewrite with an range query.
     * Use 'earliest arrive time' rather than simply use 'travel time' property at departureTime
     * Because there exist cases that 'a delay before departureTime decrease the time of
     * arrival'.(eg. wait until road not jammed, See Dreyfus 1969, page 29)
     * This makes the arrive-time-function non-decreasing, thus guarantee FIFO property of this temporal network.
     * This property is the foundational assumption to found earliest arrive time with this algorithm.
     * @param r road.
     * @param departureTime time start from r's start node.
     * @return earliest arrive time to r's end node when departure from r's start node at departureTime.
     */
    private int getEarliestArriveTime(Relationship r, int departureTime) throws UnsupportedOperationException {
        int minArriveTime = Integer.MAX_VALUE;
        for(int curT = departureTime; curT<minArriveTime; curT++){
            assert r.hasProperty( travelTimePropertyKey ) : new UnsupportedOperationException();
            Object tObj = r.getTemporalProperty( travelTimePropertyKey, TimePoint.unix(departureTime));
            assert tObj != null : new UnsupportedOperationException();
            int period = (Integer) tObj;
            if (curT + period < minArriveTime) {
                minArriveTime = curT + period;
            }
        }
        return minArriveTime;
    }

    /**
     * use this queue as a 'set' which contains all nodes labeled 'CLOSED'.
     * use priority queue(min heap) data structure, this guarantee:
     * 1. O(1) for "peek(get node with earliest arrive time among queue)" operation
     * 2. O(log(n)) for "add" and "remove" operation(since it will adjust the heap)
     */
    private PriorityQueue<Node> minHeap = new PriorityQueue<>((o1, o2) -> {
        int t1 = (Integer) o1.getProperty(EARLIEST_ARRIVE_TIME);
        int t2 = (Integer) o2.getProperty(EARLIEST_ARRIVE_TIME);
        return Integer.compare(t1, t2);
    });
    /**
     * this is an O(1) implementation, because:
     * 1. node status only transfer from NotCalculate to Calculating, never back.
     * 2. we use an [minimum heap(PriorityQueue in Java)] data structure.
     * BE CAREFUL! this implementation is only valid based on the assumption [1]!
     * therefore it may only work for Dijkstra Shortest Path algorithm.
     * DO VERIFY this when using algorithm other than Dijkstra.
     * @return node in set( status == Calculating ) and has earliest arrive time among the set.
     */
    private Node findSmallestClosedNode() {
        return minHeap.peek();
    }

    /**
     * when status transfer from OPEN to CLOSE,
     * will add the node to heap.
     */
    private void setStatus(Node node, Status status){
        node.setProperty(EARLIEST_ARRIVE_NODE_STATUS, status.value());
        if (status == Status.Calculating) {
            minHeap.add(node);
        }else if( status == Status.Calculated){
            minHeap.remove(node);
        }
    }

    private Status getStatus(final Node node){
        Object status = node.getProperty(EARLIEST_ARRIVE_NODE_STATUS);
        assert status != null;
        return Status.valueOf((Integer) status);
    }

    private void setArriveTime( Node node, int value ) {
        node.setProperty( EARLIEST_ARRIVE_TIME, value );
    }

    private void setParent(Node node, Node parent) {
        node.setProperty( EARLIEST_ARRIVE_PATH_PARENT, parent.getId() );
    }

    private enum Status{
        NotCalculate(0), Calculating(1), Calculated(2);
        private int value;
        Status(int value){
            this.value=value;
        }
        public int value(){
            return this.value;
        }

        public static Status valueOf(int status) {
            switch (status){
                case 0: return NotCalculate;
                case 1: return Calculating;
                case 2: return Calculated;
            }
            throw new RuntimeException("no such value in Status!");
        }
    }
}
