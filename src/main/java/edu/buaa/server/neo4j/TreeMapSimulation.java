package edu.buaa.server.neo4j;

import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.benchmark.transaction.ImportTemporalDataTx;
import edu.buaa.server.Neo4jKernelTcpServer;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Created by song on 16-2-26.
 */
public class TreeMapSimulation extends Neo4jKernelTcpServer.Neo4jBackEnd
{
    private String key(String prefix, int time){
        return prefix+":"+time;
    }

    private void set(PropertyContainer pContainer, int time, int jamStatus, int travelTime, int segCnt) {
        TreeSet<Integer> value = (TreeSet<Integer>) pContainer.getProperty("updateTime", null);
        if (value == null) {
            value = new TreeSet<>();
            value.add(time);
            pContainer.setProperty("updateTime", value);
            pContainer.setProperty(key("travel_time", time), travelTime);
            pContainer.setProperty(key("jam_status", time), jamStatus);
            pContainer.setProperty(key("road_segcnt", time), segCnt);
        } else {
            NavigableSet<Integer> toDel = value.subSet(time, false, Integer.MAX_VALUE, false);
            for(Integer t : toDel){
                pContainer.setProperty(key("travel_time", t), null);
                pContainer.setProperty(key("jam_status", t), null);
                pContainer.setProperty(key("road_segcnt", t), null);
            }
            toDel.clear();
            value.add(time);
            pContainer.setProperty("updateTime", value);
            pContainer.setProperty(key("travel_time", time), travelTime);
            pContainer.setProperty(key("jam_status", time), jamStatus);
            pContainer.setProperty(key("road_segcnt", time), segCnt);
        }
    }

    @Override
    protected int roadEAT(long roadId, int departureTime, int endTime) throws UnsupportedOperationException {
        Relationship r = db.getRelationshipById(roadId);
        TreeSet<Integer> value = (TreeSet<Integer>) r.getProperty("updateTime", null);
        if(value!=null){
            Integer t = value.floor(departureTime);
            int minArriveT = Integer.MAX_VALUE;
            if(t!=null){
                if(t<departureTime){
                    minArriveT = departureTime+ ((Integer)r.getProperty(key("travel_time", t)));
                }
                NavigableSet<Integer> toQuery = value.subSet(departureTime, true, endTime, true);
                for(Integer time : toQuery){
                    int travelTime = (Integer)r.getProperty(key("travel_time", time));
                    if(time+travelTime<minArriveT) minArriveT = time+travelTime;
                }
                return minArriveT;
            }else{
                throw new UnsupportedOperationException();
            }
        }else{
            throw new UnsupportedOperationException();
        }
    }

    @Override
    protected AbstractTransaction.Result execute(ImportTemporalDataTx tx) {
        try(Transaction t = db.beginTx()){
            for(ImportTemporalDataTx.StatusUpdate s : tx.getData()){
                Relationship r = db.getRelationshipById(s.getRoadId());
                set(r, s.getTime(), s.getJamStatus(), s.getTravelTime(), s.getSegmentCount());
            }
            t.success();
        }
        return new AbstractTransaction.Result();
    }
}
