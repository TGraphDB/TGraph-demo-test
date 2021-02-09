//package org.act.temporal.test.singleThread;
//
//import edu.buaa.benchmark.client.DBProxy;
//import edu.buaa.client.Config;
//import edu.buaa.server.neo4j.ArraySimulationProxy;
//import org.act.temporal.test.utils.Helper;
//import org.act.temporal.test.utils.Monitor;
//import org.junit.After;
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
//import java.util.*;
//
///**
// * Created by song on 16-2-24.
// */
//public class GetPointTest {
//
//    private static Config config = new Config();
//    private static List<Integer> timeList = new ArrayList<>();
//    private float percent = 0.01f;//test count total.
//    private DBProxy proxy = config.proxy;
//    private GraphDatabaseService db;
//    private Logger logger = LoggerFactory.getLogger(GetRangeAggregateTest.class);
//    private Monitor monitor = new Monitor(logger);
//    List<Relationship> rList = new ArrayList<>();
//
//    public void prepareDB() throws FileNotFoundException {
//        db = new GraphDatabaseFactory()
//                .newEmbeddedDatabaseBuilder(config.dbPath)
//                .loadPropertiesFromFile("")
//                .newGraphDatabase();
//        try (Transaction tx = db.beginTx()) {
//            for (Relationship relationship : GlobalGraphOperations.at(db).getAllRelationships()) {
//                rList.add(relationship);
//            }
//        }
//    }
//
//
//    @Test
//    public void readTestNeo4jTemporal() throws FileNotFoundException, InterruptedException {
//        config.dbPath = Config.Default.dbPath + "-get-point";
//
//        List<File> fileList = new ArrayList<>();
//        Helper.getFileRecursive(new File(config.dataPathDir), fileList, 5);
//        fileList.sort(null);
//        Collections.shuffle(fileList);
//        for (File file : fileList) {
//            timeList.add(Helper.getFileTime(file));
//        }
//
//        prepareDB();
//        readTestRun();
//    }
//
//    @Test
//    public void readTestArraySimulation() throws FileNotFoundException, InterruptedException {
//        config.dbPath = Config.Default.dbPath + "-arraySimulationInit";
//        proxy = new ArraySimulationProxy();
//
//        for(int i=0;i<12000;i++){
//            timeList.add(12000-i);
//        }
//
//        prepareDB();
//        readTestRun();
//    }
//
//
//    public void readTestRun() throws InterruptedException {
//        int to = (int) (timeList.size() * percent);
//        logger.info("will read {} relationship",to);
//        long readCount = 0;
//        monitor.begin();
//        try (Transaction tx = db.beginTx()) {
//            for (int i = 0; i < to; i++) {
//                int time = timeList.get(i);
//                for (Relationship relationship : rList) {
//                    Integer v1= (Integer) proxy.get(relationship, "travel-time", time);
//                    Integer v2= (Integer) proxy.get(relationship, "full-status", time);
//                    Integer v3= (Integer) proxy.get(relationship, "vehicle-count", time);
//                    Integer v4= (Integer) proxy.get(relationship, "segment-count", time);
//                    logger.info("rel:{},value:{},{},{},{}", relationship.getId(),v1,v2,v3,v4);
//                    readCount += 4;
//                }
//                monitor.end(rList.size());
//                monitor.begin();
//            }
//            tx.success();
//        }
//        logger.info("read point test end, read {}",readCount);
//    }
//
//
//    @After
//    public void closeDB() {
//        if (db != null) {
//            db.shutdown();
//        }
//    }
//}