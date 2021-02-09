//package org.act.temporal.test.singleThread;
//
//import edu.buaa.benchmark.client.DBProxy;
//import edu.buaa.client.vo.RelType;
//import edu.buaa.client.Config;
//import edu.buaa.server.neo4j.Aggregator;
//import edu.buaa.server.neo4j.ArraySimulationProxy;
//import org.act.temporal.test.utils.DataImportor;
//import org.act.temporal.test.utils.Helper;
//import org.junit.After;
//import org.junit.Test;
//import org.neo4j.graphdb.Node;
//import org.neo4j.graphdb.Relationship;
//import org.neo4j.graphdb.Transaction;
//import org.neo4j.graphdb.factory.GraphDatabaseFactory;
//import org.neo4j.temporal.TimePoint;
//import org.neo4j.tooling.GlobalGraphOperations;
//
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.IOException;
//import java.io.ObjectOutputStream;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.TreeMap;
//
///**
// * Created by song on 16-2-26.
// */
////@Ignore
//public class SimpleTest {
//
//    private Config config = new Config();
//
//    @Test
//    public void go(){
//        config.dbPath += "-simple-test";
//        Helper.deleteExistDB(config);
//        config.db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(config.dbPath));
////        DBProxy proxy = new TreeMapKVSimulationProxy();
//        DBProxy proxy = new ArraySimulationProxy();
//        config.proxy  = proxy;
//        try(Transaction tx = config.db.beginTx()){
//            Node node = config.db.createNode();
//            proxy.set(node, "key", 2010, 10);
//            proxy.set(node, "key", 2012, 12);
////            System.out.println(node.getDynPropertyPointValue("key", 2011));
//            System.out.println(proxy.get(node, "key", 2011));
//            System.out.println(proxy.getAggregate(node, "key", 2008, 2008, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2008, 2009, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2008, 2010, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2008, 2011, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2008, 2012, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2008, 2013, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2010, 2010, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2010, 2011, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2010, 2012, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2010, 2013, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2011, 2011, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2011, 2012, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2011, 2013, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2012, 2012, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2012, 2013, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2013, 2013, aggregator));
//            System.out.println(proxy.getAggregate(node, "key", 2013, 2014, aggregator));
//
//            tx.success();
//        }
//        config.db.shutdown();
//    }
//
//    Aggregator aggregator = new Aggregator() {
//        private int sum =0;
//        @Override
//        public String result() {
//            int result = sum;
//            sum=0;
//            return result+"";
//        }
//
//        @Override
//        public boolean add(int time, Object value) {
//            sum += (int)value;
//            return true;
//        }
//    };
//
//    @Test
//    public void test(){
//        TreeMap<Integer,Integer> map = new TreeMap<>();
//        for(int j=1;j<20;j++) {
//            for (int i = 0; i < 1024 * 1024 * j; i++) {
//                map.put(i, j);
//            }
//            try {
//                ByteArrayOutputStream bo = new ByteArrayOutputStream();
//                ObjectOutputStream so = new ObjectOutputStream(bo);
//                so.writeObject(map);
//                so.flush();
//                System.out.println(bo.toByteArray().length);
////            System.out.println("encode result:" + Helper.bytesToHex(bo.toByteArray()));
////            System.out.println(new String(Base64.getEncoder().encode(bo.toByteArray())));
////            return new String(bo.toByteArray());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//    }
//
//
//    @Test
//    public void osname(){
//        for(int i=0;i<1;i++){
//            System.out.println(System.getProperty("os.name"));
//            System.out.println(System.getProperty("user.name"));
//        }
//        throw new RuntimeException("reach here?");
//    }
//
//    @Test
//    public void ifGetRelationshipByIdAccessDisk(){
//        config.dbPath += "-simple-test";
//        Helper.deleteExistDB(config);
//        config.db = new GraphDatabaseFactory()
//                .newEmbeddedDatabaseBuilder(config.dbPath)
//                .loadPropertiesFromFile("")
//                .newGraphDatabase();
//        try(Transaction tx = config.db.beginTx()){
//            Node node = config.db.createNode();
//            Node node1 = config.db.createNode();
//            Relationship relationship = node1.createRelationshipTo(node, RelType.ROAD_TO);
//            System.out.println(relationship.getId());
//            tx.success();
//        }
//        config.db.shutdown();
//
//        config.db = new GraphDatabaseFactory()
//                .newEmbeddedDatabaseBuilder(config.dbPath)
//                .loadPropertiesFromFile("")
//                .newGraphDatabase();
//        try(Transaction tx = config.db.beginTx()){
//            Relationship relationship = config.db.getRelationshipById(0);
//            System.out.println(relationship.getId());
//            tx.success();
//        }
//    }
//
//
//    @Test
//    public void fixNotShutdownBug() throws IOException {
//        config.dbPath += "-simple-test";
//        Helper.deleteExistDB(config);
//        config.db = new GraphDatabaseFactory()
//                .newEmbeddedDatabaseBuilder(config.dbPath)
//                .loadPropertiesFromFile("")
//                .newGraphDatabase();
//
//        DataImportor.importNetwork(config.db);
//
//        List<Relationship> rList = new ArrayList<>();
//
//        try (Transaction tx = config.db.beginTx()) {
//            for(Relationship relationship: GlobalGraphOperations.at(config.db).getAllRelationships()){
//                rList.add(relationship);
//                break;
//            }
//            Relationship r = rList.get(0);
//            r.setProperty("travel-time",0);
//            r.setProperty("full-status",0);
//            r.setProperty("vehicle-count",0);
//            r.setProperty("segment-count",0);
//            tx.success();
//        }
//
//
//
////        try(Transaction tx = config.db.beginTx()){
////            Node node = config.db.createNode();
////            Node node1 = config.db.createNode();
////            Relationship relationship = node1.createRelationshipTo(node, RelType.ROAD_TO);
////            System.out.println(relationship.getId());
////            tx.success();
////        }
////        config.db.shutdown();
//    }
//
//    @Test
//    public void ifBugFixed() throws IOException {
//        config.dbPath += "-get-point";
//
//        config.db = new GraphDatabaseFactory()
//                .newEmbeddedDatabaseBuilder(config.dbPath)
//                .loadPropertiesFromFile("")
//                .newGraphDatabase();
//
//        int normalCount=0;
//        long nulCount=0;
//        try (Transaction tx = config.db.beginTx()) {
//            for(Relationship r: GlobalGraphOperations.at(config.db).getAllRelationships()){
//                long id = r.getId();
//                Integer v1 = (Integer) r.getTemporalProperty("travel-time",new TimePoint(1011082355));
//                Integer v2 = (Integer) r.getTemporalProperty("full-status",new TimePoint(1011082355));
//                Integer v3 = (Integer) r.getTemporalProperty("vehicle-count",new TimePoint(1011082355));
//                Integer v4 = (Integer) r.getTemporalProperty("segment-count",new TimePoint(1011082355));
//
//                if(v1==null && v2==null && v3==null && v4==null){
//                    nulCount++;
//                }else if(v1!=null && v2!=null && v3!=null && v4!=null){
//                    normalCount++;
//                }else{
//                    config.logger.info("error!{},{},{},{}",v1,v2,v3,v4);
//                }
////                config.logger.info("{},{}",r.getId(),r.getDynPropertyPointValue("travel-time",1011082355));
////                config.logger.info("{},{}",r.getId(),r.getDynPropertyPointValue("full-status",1011082355));
////                config.logger.info("{},{}",r.getId(),r.getDynPropertyPointValue("vehicle-count",1011082355));
////                config.logger.info("{},{}",r.getId(),r.getDynPropertyPointValue("segment-count", 1011082355));
////                if(index>readlen)break;
////                else index++;
//            }
//            tx.success();
//        }
//        config.logger.info("null:{},normal:{}",nulCount,normalCount);
//
//    }
//    @After
//    public void closedb(){
//        if(config.db!=null) config.db.shutdown();
//    }
//}
