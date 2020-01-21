//package org.act.temporal.test.singleThread;
//
//import edu.buaa.client.vo.RelType;
//import edu.buaa.benchmark.client.DBProxy;
//import edu.buaa.benchmark.client.neo4j.TemporalSimulationProxy;
//import org.act.temporal.test.utils.Helper;
//import org.act.temporal.test.utils.Monitor;
//import org.junit.*;
//import org.junit.runner.RunWith;
//import org.junit.runners.Parameterized;
//import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.graphdb.Node;
//import org.neo4j.graphdb.Relationship;
//import org.neo4j.graphdb.Transaction;
//import org.neo4j.graphdb.factory.GraphDatabaseFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.UUID;
//
///**
// * Created by song on 16-2-23.
// */
//@Ignore
//@RunWith(Parameterized.class)
//public class GetTest {
//    private GraphDatabaseService db;
//    private DBProxy temporalDB=new TemporalSimulationProxy();
//    private Logger logger= LoggerFactory.getLogger(GetTest.class);
//    private Monitor monitor=new Monitor(logger);
//
//    private Transaction tx;
//    private int n,p,t,v;
//    /**
//     *  n: node/relationship num in this test.
//     *  p: temporal property num on each node/relationship
//     *  t: each temporal property has t timestamp value
//     *  v: each value size is v bytes
//     */
//    public GetTest(int n, int p, int t, int v){
//        this.n = n;
//        this.p = p;
//        this.t = t;
//        this.v = v;
//    }
//
//    @Parameterized.Parameters
//    public static Collection primeNumbers() {
//        return Arrays.asList(new Object[][]{
//                {10,10,10,10},
//                {10,10,10,100},
//                {10,10,10,1000},
//                {10,10,10,10000},
//                {10,10,10,100000}
//        });
//    }
//
//
//    @BeforeClass
//    public static void init(){
//
//    }
//
//    @Before
//    public void prepare(){
//        File dir = new File("neo4j-temporal-demo");
//        if (dir.exists()){
//            Helper.deleteAllFilesOfDir(dir);
//        }
//        dir.mkdir();
//        db = new GraphDatabaseFactory().newEmbeddedDatabase(dir.getAbsolutePath());
////        tx = db.beginTx();
//
//    }
//
//
//    @Test
//    public void create(){
//        monitor.begin();
//
//        try(Transaction tx = db.beginTx()){
//            for(int i=0;i<this.n;i++) {
//                Node node = db.createNode();
//                for (int j = 0; j < this.p; j++) {
//                    String key = UUID.randomUUID().toString();
//                    for (int k = 0; k < this.t; k++) {
//
//                        temporalDB.set(node, key, k, Helper.getString(this.v));
//                    }
//                }
//            }
//            tx.success();
//        }
//        monitor.end(this.n * this.p * this.t);
//    }
//
//
//    @Test
//    public void simple(){
//        Node node;
//        long id=0;
//        try(Transaction tx = db.beginTx()){
//            node = db.createNode();
//            logger.info("nodeid:{},obj:{}",node.getId(),node);
//            id = node.getId();
//            node.setProperty("abc","---");
//            tx.success();
//        }
//        try(Transaction tx = db.beginTx()){
//            Node node1 = db.getNodeById(id);
//            logger.info("nodeid:{},obj:{}",node.getId(),node);
//            logger.info("equal? {}", node == node1);
//            logger.info("abc:{}", node1.getProperty("abc"));
//            Relationship relationship = node.createRelationshipTo(node1, RelType.ROAD_TO);
//            logger.info("rid:{}",relationship.getId());
//            db.getRelationshipById(0);
//            node.delete();
//            node1.setProperty("abc","123");
//            tx.success();
//        }
//    }
//
//    @After
//    public void clean(){
//
//        if(db!=null)db.shutdown();
//    }
//
//}
