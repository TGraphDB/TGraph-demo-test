//package edu.buaa.server.neo4j;
//
//import edu.buaa.benchmark.transaction.AbstractTransaction;
//import edu.buaa.benchmark.transaction.ImportTemporalDataTx;
//import edu.buaa.model.StatusUpdate;
//import edu.buaa.server.Neo4jKernelTcpServer;
//import org.neo4j.graphdb.PropertyContainer;
//import org.neo4j.graphdb.Relationship;
//import org.neo4j.graphdb.Transaction;
//
//import java.util.ArrayList;
//
///**
// * Created by song on 16-2-27.
// */
//public class ArraySimulation extends Neo4jKernelTcpServer.Neo4jBackEnd
//{
//    private void set(PropertyContainer pContainer, String key, int time, int content) {
//        ArrayList<Integer> value = (ArrayList<Integer>) pContainer.getProperty(key, null);
//        if (value == null) {
//            value = new ArrayList<>();
//            value.add(time);
//            value.add(content);
//            pContainer.setProperty(key, value);
//        } else {
//            int len = value.size();
//            for(int i=0; i<len; i+=2){
//                if(value.get(i)>=time){
//                    value.set(i, time);
//                    value.set(i+1, content);
//                    pContainer.setProperty(key, value.subList(0, i+2));
//                    return;
//                }
//            }
//            value.add(time);
//            value.add(content);
//            pContainer.setProperty(key, value);
//        }
//    }
//
//    @Override
//    protected int roadEAT(long roadId, int departureTime, int endTime) throws UnsupportedOperationException {
//        ArrayList<Integer> value = (ArrayList<Integer>) db.getRelationshipById(roadId).getProperty("travel_time", null);
//        if(value!=null && value.size()>0 && value.get(0)<=departureTime){
//            int i=0;
//            while(i<value.size() && value.get(i)<departureTime){
//                i+=2;
//            }
//            if(i>=value.size()){
//                int travelT = value.get(i-1);
//                return departureTime+travelT;
//            }else{
//                int minArriveT = Integer.MAX_VALUE;
//                if(value.get(i)>departureTime) {
//                    int travelT = value.get(i-1);
//                    minArriveT = departureTime + travelT;
//                }
//                for(int j=i; j<value.size() && value.get(j)<=endTime; j+=2){
//                    int t = value.get(j);
//                    int travelT = value.get(j+1);
//                    if(t+travelT<minArriveT){
//                        minArriveT = t+travelT;
//                    }
//                }
//                return minArriveT;
//            }
//        }else{
//            throw new UnsupportedOperationException();
//        }
//    }
//
//    @Override
//    protected AbstractTransaction.Result execute(ImportTemporalDataTx tx) {
//        try(Transaction t = db.beginTx()){
//            for(StatusUpdate s : tx.getData()){
//                Relationship r = db.getRelationshipById(s.getRoadId());
//                set(r, "travel_time", s.getTime(), s.getTravelTime());
//                set(r, "jam_status", s.getTime(), s.getJamStatus());
//                set(r, "road_segcnt", s.getTime(), s.getSegmentCount());
//            }
//            t.success();
//        }
//        return new AbstractTransaction.Result();
//    }
//}
