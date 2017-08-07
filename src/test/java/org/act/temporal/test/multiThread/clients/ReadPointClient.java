package org.act.temporal.test.multiThread.clients;

import org.act.neo4j.temporal.demo.Config;
import org.act.temporal.test.utils.Monitor;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by song on 16-2-26.
 */

public class ReadPointClient extends Client {
    private List<Integer> timeList;

    public ReadPointClient(Config config, List<Integer> timeList) {
        super(config);
        this.timeList = timeList;
    }

    @Override
    public void run(){
        currentThread().setName("Neo4jTemporalTest:"+this.getClass().getSimpleName()+"{"+currentThread().getId()+"}");
        List<Relationship> rList = new ArrayList<>();
        try(Transaction tx = db.beginTx()){
            for(Relationship relationship: GlobalGraphOperations.at(db).getAllRelationships()) {
                rList.add(relationship);
            }
            tx.success();
        }
        Collections.shuffle(rList);
        while(!exit){
            int index=0;
            for(Relationship relationship:rList){
                int time = timeList.get(index);
                int tmp = index % 4;
                if (tmp == 0) {
                    monitor.snapShot(Monitor.BEGIN);
                    try (Transaction tx = db.beginTx()) {
                        proxy.get(relationship, "travel-time", time);
                        tx.success();
                    }
                    monitor.snapShot(Monitor.END);
                }else if(tmp == 1){
                    monitor.snapShot(Monitor.BEGIN);
                    try(Transaction tx = db.beginTx()){
                        proxy.get(relationship, "full-status", time);
                        tx.success();
                    }
                    monitor.snapShot(Monitor.END);
                }else if(tmp == 2){
                    monitor.snapShot(Monitor.BEGIN);
                    try(Transaction tx = db.beginTx()){
                        proxy.get(relationship, "vehicle-count", time);
                        tx.success();
                    }
                    monitor.snapShot(Monitor.END);
                }else{
                    monitor.snapShot(Monitor.BEGIN);
                    try(Transaction tx = db.beginTx()){
                        proxy.get(relationship, "segment-count", time);
                        tx.success();
                    }
                    monitor.snapShot(Monitor.END);
                }
                index++;
                if(index==timeList.size()) index=0;
                if(exit) return;
            }
        }
    }
}