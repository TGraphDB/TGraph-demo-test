//package org.act.temporal.test.multiThread.clients;
//
//import edu.buaa.client.Config;
//import org.act.temporal.test.vo.Line;
//import org.neo4j.graphdb.Relationship;
//import org.neo4j.graphdb.Transaction;
//
//import java.util.concurrent.BlockingQueue;
//
///**
// * Created by song on 16-2-26.
// */
//public class ConsumerWriteClient extends Client{
//
//    private BlockingQueue<Line> queue;
//    private String name;
//    public ConsumerWriteClient(Config config, BlockingQueue<Line> queue, String name) {
//        super(config);
//        this.queue = queue;
//        this.name = name;
//    }
//
//    @Override
//    public void run() {
//        int count=0;
//        currentThread().setName("Neo4jTemporalTest:"+this.getClass().getSimpleName()+"{"+currentThread().getId()+"}");
//        while(!exit) {
//            try (Transaction tx = db.beginTx()) {
//                for (int writeCount = 0; writeCount < 100 && !exit; writeCount++) {
//                    Line line = queue.take();
//                    Relationship relationship = line.roadChain.getRelationship(db);
//                    if (relationship != null) {
//                        proxy.set(relationship, this.name, line.time, line.value);
//                    } else {
//                        System.out.println(line.roadChain);
//                    }
//                }
//                tx.success();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//}
