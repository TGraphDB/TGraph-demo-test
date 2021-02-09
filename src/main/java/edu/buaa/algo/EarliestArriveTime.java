package edu.buaa.algo;


import com.alibaba.fastjson.annotation.JSONType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class EarliestArriveTime {
    private final Map<Long, NodeCross> nodeCrossMap = new HashMap<>();
    private final long start;
    private final int startTime;
    protected final int endTime;

    public EarliestArriveTime(long startId, int startTime, int travelTime){
        this.start = startId;
        this.startTime = startTime;
        this.endTime = startTime + travelTime;
    }

    public Set<NodeCross> run() {
        NodeCross startNode = getNodeCross(start);
        setStatus(startNode, Status.Calculating);
        startNode.arriveTime = startTime;
        startNode.parent = start;

        Set<NodeCross> result = new HashSet<>();
        NodeCross node;
        while ((node = smallestCalculatingNode())!=null) {
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
    private void loopAllNeighborsUpdateArriveTime(NodeCross node, Set<NodeCross> result) {
        setStatus( node, Status.Calculated );
        int curTime = node.arriveTime;
        if( curTime > endTime ) return;
        result.add( node );
        for( Long rId : getAllOutRoads( node.getId() )){
            NodeCross neighbor = getNodeCross( getEndNodeId( rId ));
            int arriveTime;
            try {
                switch (neighbor.status) {
                    case NotCalculate:
                        neighbor.arriveTime = getEarliestArriveTime(rId, curTime);
                        neighbor.parent = node.getId();
                        setStatus(neighbor, Status.Calculating);
                        break;
                    case Calculating:
                        arriveTime = getEarliestArriveTime(rId, curTime);
                        if (neighbor.arriveTime > arriveTime) {
                            neighbor.arriveTime = arriveTime;
                            neighbor.parent = node.getId();
                        }
                        break;
                }
            }catch (UnsupportedOperationException ignore){}
        }
    }

    /**
     * Use 'earliest arrive time' rather than simply use 'travel time' property at departureTime
     * Because there exist cases that 'a delay before departureTime decrease the time of
     * arrival'.(eg. wait until road not jammed, See Dreyfus 1969, page 29)
     * This makes the arrive-time-function non-decreasing, thus guarantee FIFO property of this temporal network.
     * This property is the foundational assumption to found earliest arrive time with this algorithm.
     * @param roadId road id.
     * @param departureTime time start from r's start node.
     * @return earliest arrive time to r's end node when departure from r's start node at departureTime.
     */
    protected abstract int getEarliestArriveTime(Long roadId, int departureTime) throws UnsupportedOperationException;
    protected abstract Iterable<Long> getAllOutRoads( long nodeId );
    protected abstract long getEndNodeId( long roadId );

    private HashSet<NodeCross> calculatingNodes = new HashSet<>();

    private NodeCross smallestCalculatingNode() {
        NodeCross min = null;
        int minTime = Integer.MAX_VALUE;
        for(NodeCross n : calculatingNodes){
            if(n.arriveTime < minTime) {
                minTime = n.arriveTime;
                min = n;
            }
        }
        return min;
    }

    private void setStatus(NodeCross node, Status status){
        node.status = status;
        if (status == Status.Calculating) {
            calculatingNodes.add(node);
        }else if( status == Status.Calculated){
            calculatingNodes.remove(node);
        }
    }

    private NodeCross getNodeCross(long id) {
        NodeCross node = nodeCrossMap.get(id);
        if( node == null ){
            node = new NodeCross(id);
            nodeCrossMap.put(id, node);
        }
        return node;
    }

    protected enum Status{ NotCalculate, Calculating, Calculated }

    @JSONType(ignores = {"status"})
    public static class NodeCross {
        public long id;
        int arriveTime = Integer.MAX_VALUE;
        long parent;
        private Status status = Status.NotCalculate;
        NodeCross(){}
        NodeCross(long id) {
            this.id = id;
        }
        public NodeCross(long id, int arriveTime) {
            this.id = id;
            this.arriveTime = arriveTime;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public int getArriveTime() {
            return arriveTime;
        }

        public void setArriveTime(int arriveTime) {
            this.arriveTime = arriveTime;
        }

        public long getParent() {
            return parent;
        }

        public void setParent(long parent) {
            this.parent = parent;
        }

        @Override
        public String toString() {
            return "NodeCross{" +
                    "id=" + id +
                    ", arriveTime=" + arriveTime +
                    ", parent=" + parent +
                    '}';
        }
    }
}
