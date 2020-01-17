//package org.act.temporal.test.multiThread.clients;
//
//import org.act.tgraph.demo.client.Config;
//import org.act.tgraph.demo.benchmark.client.neo4j.Aggregator;
//import org.act.temporal.test.utils.Monitor;
//import org.neo4j.graphdb.Relationship;
//import org.neo4j.graphdb.Transaction;
//import org.neo4j.tooling.GlobalGraphOperations;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
///**
// * Created by song on 16-2-26.
// */
//public class ReadRangeAggregateClient extends Client{
//    private List<TimePair> timePairs = new ArrayList<>();
//    public ReadRangeAggregateClient(Config config, List<Integer> timeList) {
//        super(config);
//        for(int i=1;i<timeList.size();i+=2){
//            timePairs.add(new TimePair(timeList.get(i - 1), timeList.get(i)));
//        }
//    }
//
//    @Override
//    public void run(){
//        currentThread().setName("Neo4jTemporalTest:"+this.getClass().getSimpleName()+"{"+currentThread().getId()+"}");
//        List<Relationship> rList = new ArrayList<>();
//        try(Transaction tx = db.beginTx()){
//            for(Relationship relationship: GlobalGraphOperations.at(db).getAllRelationships()) {
//                rList.add(relationship);
//            }
//            tx.success();
//        }
//        Collections.shuffle(rList);
//        while(!exit){
//            int index=0;
//            for(Relationship relationship:rList){
//                TimePair time = timePairs.get(index);
//                int tmp = index % 4;
//                if (tmp == 0) {
//                    monitor.snapShot(Monitor.BEGIN);
//                    try (Transaction tx = db.beginTx()) {
//                        proxy.getAggregate(relationship, "travel-time", time.from, time.to, Aggregator.COUNT);
//                        tx.success();
//                    }
//                    monitor.snapShot(Monitor.END);
//                }else if(tmp == 1){
//                    monitor.snapShot(Monitor.BEGIN);
//                    try(Transaction tx = db.beginTx()){
//                        proxy.getAggregate(relationship, "full-status", time.from, time.to, Aggregator.COUNT);
//                        tx.success();
//                    }
//                    monitor.snapShot(Monitor.END);
//                }else if(tmp == 2){
//                    monitor.snapShot(Monitor.BEGIN);
//                    try(Transaction tx = db.beginTx()){
//                        proxy.getAggregate(relationship, "vehicle-count", time.from, time.to, Aggregator.MAX_VALUE);
//                        tx.success();
//                    }
//                    monitor.snapShot(Monitor.END);
//                }else{
//                    monitor.snapShot(Monitor.BEGIN);
//                    try(Transaction tx = db.beginTx()){
//                        proxy.getAggregate(relationship, "segment-count", time.from, time.to, Aggregator.MAX_VALUE);
//                        tx.success();
//                    }
//                    monitor.snapShot(Monitor.END);
//                }
//                index++;
//                if(index==timePairs.size()) index=0;
//                if(exit) return;
//            }
//        }
//    }
//
//    private static class TimePair{
//        public int from,to;
//        TimePair(int t1, int t2){
//            if(t1>t2){
//                this.from = t2;
//                this.to = t1;
//            }else{
//                this.from = t1;
//                this.to = t2;
//            }
//        }
//    }
//
//}