package edu.buaa.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import edu.buaa.algo.EarliestArriveTime;
import edu.buaa.utils.Helper;
import org.act.temporalProperty.query.TimePointL;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.temporal.TemporalRangeQuery;

public class EarliestArriveTimeTGraphKernel extends EarliestArriveTime {
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
        return () -> Iterators.transform(node.getRelationships(Direction.OUTGOING).iterator(), Relationship::getId);
    }

    @Override
    protected long getEndNodeId(long roadId) {
        return db.getRelationshipById(roadId).getEndNode().getId();
    }
}
