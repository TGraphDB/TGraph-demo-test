//package org.act.temporal.test.singleThread;
//
//import edu.buaa.benchmark.client.DBProxy;
//import edu.buaa.server.neo4j.Aggregator;
//import edu.buaa.client.Config;
//import org.act.temporal.test.utils.Helper;
//import org.act.temporal.test.utils.Monitor;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.graphdb.Relationship;
//import org.neo4j.graphdb.Transaction;
//import org.neo4j.graphdb.factory.GraphDatabaseFactory;
//import org.neo4j.tooling.GlobalGraphOperations;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
///**
// * Created by song on 16-2-24.
// */
//public class GetRangeAggregateTest {
//
//    private static List<TimePair> timeList = new ArrayList<>();
//    private static Config config = new Config(){
//        public String dbPath = Config.Default.dbPath+"-get-range";
//    };
//
//    private float percent = 0.1f;//test count total.
//    private DBProxy proxy = config.proxy;
//    private GraphDatabaseService db;
//    private Logger logger= LoggerFactory.getLogger(GetRangeAggregateTest.class);
//    private Monitor monitor=new Monitor(logger);
//
//    @BeforeClass
//    public static void prepareTimeList(){
//        List<File> fileList = new ArrayList<>();
//        Helper.getFileRecursive(new File(config.dataPathDir), fileList, 5);
//        fileList.sort(null);
//        Collections.shuffle(fileList);
//        for(int i=1;i<fileList.size();i+=2){
//            int t1 = Helper.getFileTime(fileList.get(i));
//            int t2 = Helper.getFileTime(fileList.get(i-1));
//            timeList.add(new TimePair(t1,t2));
//        }
//    }
//
//    @Before
//    public void prepareDB() throws FileNotFoundException {
//        db = new GraphDatabaseFactory()
//                .newEmbeddedDatabaseBuilder(config.dbPath)
//                .loadPropertiesFromFile("")
//                .newGraphDatabase();
//    }
//
//    @Test
//    public void readTest() throws InterruptedException {
//        int to = (int) (timeList.size()*percent);
//        long readCount = 0;
//        monitor.begin();
//        try(Transaction tx = db.beginTx()) {
//            for (int i = 0; i < to; i++) {
//                TimePair time = timeList.get(i);
//                for(Relationship relationship: GlobalGraphOperations.at(db).getAllRelationships()){
//                    proxy.getAggregate(relationship, "travel-time", time.from, time.to, Aggregator.COUNT);
//                    proxy.getAggregate(relationship, "full-status", time.from, time.to, Aggregator.COUNT);
//                    proxy.getAggregate(relationship, "vehicle-count", time.from, time.to, Aggregator.MAX_VALUE);
//                    proxy.getAggregate(relationship, "segment-count", time.from, time.to, Aggregator.MAX_VALUE);
//                    readCount+=4;
//                }
//            }
//            tx.success();
//        }
//        monitor.end(readCount);
//    }
//
//    @After
//    public void closeDB(){
//        db.shutdown();
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
