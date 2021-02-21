package simple.tgraph.kernel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.ParserConfig;
import edu.buaa.benchmark.client.DBProxy;
import edu.buaa.benchmark.transaction.SnapshotQueryTx;
import edu.buaa.utils.Helper;
import edu.buaa.utils.TrafficMultiFileReader;
import org.act.temporalProperty.query.TimePointL;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.temporal.TemporalRangeQuery;
import org.neo4j.temporal.TimePoint;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SimpleTest {

    @Test
    public void pairTest() throws IOException {
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
        try(BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\BlackCat\\Desktop\\ddd.txt"))){
            String big = reader.readLine();
            DBProxy.ServerResponse res = JSON.parseObject(big, new TypeReference<DBProxy.ServerResponse>() {});
            SnapshotQueryTx.Result r = (SnapshotQueryTx.Result) res.getResult();

            System.out.println(r.getRoadStatus());
        }
    }

    @Test
    public void cypherTest(){
        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( new File("Z:\\TEMP-CYPHER") );
        try{
            Random rnd = new Random();
            try(Transaction tx = db.beginTx()){
                for(int i=0; i<100; i++){
                    Node n = db.createNode(DynamicLabel.label("User"));
                    n.setProperty("name", "n"+i);
                    n.setProperty("name1", "m1."+i);
                    n.setProperty("val", i);
                    if(n.getId()>20) for(int j=0; j<rnd.nextInt(3); j++){
                        int id = rnd.nextInt(Math.toIntExact(n.getId()));
                        n.createRelationshipTo(db.getNodeById(id), DynamicRelationshipType.withName("F"));
                    }
                    if(n.getId()>2 && rnd.nextBoolean()){
                        n.createRelationshipTo(db.getNodeById(rnd.nextInt((int) n.getId())), DynamicRelationshipType.withName("FF"));
                    }
                }
                tx.success();
            }
            try(Transaction tx = db.beginTx()){
                Schema schema = db.schema();
                for(IndexDefinition id : schema.getIndexes()){
                    System.out.println(schema.getIndexState(id));
                }
                IndexDefinition indexDefinition = schema.indexFor(DynamicLabel.label("User"))
                        .on("name")
                        .create();
                IndexDefinition indexDefinition1 = schema.indexFor(DynamicLabel.label("User"))
                        .on("name1")
                        .create();
//                schema.awaitIndexOnline(indexDefinition, 1, TimeUnit.MINUTES);
                tx.success();
            }
            try(Transaction tx = db.beginTx()){
                Result r = db.execute("MATCH(n:User)--(m:User) WHERE n.name='n88' OR m.name1='m1.80' RETURN n.name, n.val");
                System.out.println(r.getExecutionPlanDescription());
                while (r.hasNext()){
                    Map<String, Object> row = r.next();
                    System.out.println(row);
                }
//                System.out.println(r.resultAsString());
                tx.success();
            }

        }finally {
            db.shutdown();
        }
    }

    @Test
    public void cypherTest1(){
        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( new File("Z:\\TEMP") );
        try{
//            try(Transaction tx = db.beginTx()){
//                for(Node n : GlobalGraphOperations.at(db).getAllNodes()){
//                    System.out.print(n.getId()+" "+n.getDegree(Direction.INCOMING)+"(");
//                    for(Relationship r : n.getRelationships(Direction.INCOMING)){
//                        Node f = r.getOtherNode(n);
//                        System.out.print(f.getId()+" ");
//                    }
//                    System.out.print(") "+n.getDegree(Direction.OUTGOING)+"(");
//                    for(Relationship r : n.getRelationships(Direction.OUTGOING)){
//                        Node f = r.getOtherNode(n);
//                        System.out.print(f.getId()+" ");
//                    }
//                    System.out.println();
//                }
//                tx.success();
//            }

//            try(Transaction tx = db.beginTx()){
////                for(Node n : GlobalGraphOperations.at(db).getAllNodes()){
////                    System.out.println("{id:'"+n.getId()+"'},");
////                }
//                for(Relationship r : GlobalGraphOperations.at(db).getAllRelationships()){
//                    System.out.println(r.getStartNode().getId()+","+r.getEndNode().getId()+","+r.getType().name());
//                }
////                for(Relationship r : GlobalGraphOperations.at(db).getAllRelationships()){
////                    System.out.println("{source:'"+r.getStartNode().getId()+"',target:'"+r.getEndNode().getId()+"'},");
////                }
//                tx.success();
//            }
//            try(Transaction tx = db.beginTx()){
//                Result r = db.execute("MATCH(n:User)WHERE n.name='n88' RETURN n.name, n.val");
//                System.out.println(r.getExecutionPlanDescription());
//                while (r.hasNext()){
//                    Map<String, Object> row = r.next();
//                    System.out.println(row);
//                }
////                System.out.println(r.resultAsString());
//                tx.success();
//            }

        }finally {
            db.shutdown();
        }
    }

    @Test
    public void cypherTest2(){
        System.setProperty("pickBestPlan.VERBOSE", "true");
        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( new File("Z:\\TEMP") );
        String q = "p=(a)--(b)--(c)";
        try{
            try(Transaction tx = db.beginTx()){
                Result r = db.execute("MATCH "+q+" WHERE id(a)=25 AND id(c)=65 RETURN b.val+100");//WHERE id(a)<id(b)
                System.out.println(r.getExecutionPlanDescription());
                while (r.hasNext()){
                    Map<String, Object> row = r.next();
                    System.out.println(row);
                }
//                System.out.println(r.resultAsString());
                tx.success();
            }

        }finally {
            db.shutdown();
        }
    }

    @Test
    public void temporalQueryGraph(){
        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( new File("Z:\\TEMP") );
        try{
            try(Transaction tx = db.beginTx()){
//                Result r = db.execute("MATCH ()-[r:ROAD_TO]->() RETURN r.name, TP_VALUE_AT(r.travel_time, "+Helper.timeStr2int("201006280800")+")");
//                Result r = db.execute("MATCH ()-[r:ROAD_TO]->() WHERE TP_VALUE_AT(r.travel_time, 1277683200)>300 RETURN r.name, TP_VALUE_AT(r.travel_time, "+Helper.timeStr2int("201006280800")+")");
                int startTime = Helper.timeStr2int("201005020800");
                int endTime = Helper.timeStr2int("201005021200");
                Result r = db.execute("MATCH (a)-[r:ROAD_TO]->(b) " +
                        "WHERE tp_within_exists(r.travel_time,"+startTime+","+endTime+", 100, 200) " +
//                        "AND tp_within_exists(r.jam_status,1,2, 100, 200) " +
                        "RETURN id(r), r.name, TP_VALUE_AT(r.travel_time, "+Helper.timeStr2int("201006280800")+")");
                System.out.println(r.getExecutionPlanDescription());
//                while (r.hasNext()){
//                    Map<String, Object> row = r.next();
//                    System.out.println(row);
//                }
                System.out.println(r.resultAsString());
                tx.success();
            }
        }finally {
            db.shutdown();
        }
    }

    @Test
    public void entityTemporalConditionQuery(){
        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( new File("Z:\\TEMP") );
        try{
            try(Transaction tx = db.beginTx()){
                int startTime = Helper.timeStr2int("201005020800");
                int endTime = Helper.timeStr2int("201005021200");
                Result r = db.execute("MATCH ()-[r:ROAD_TO]->() WHERE tp_within_exists(r.travel_time,"+startTime+", "+endTime+", 100, 200) RETURN r.name");
                System.out.println(r.getExecutionPlanDescription());
//                while (r.hasNext()){
//                    Map<String, Object> row = r.next();
//                    System.out.println(row);
//                }
                System.out.println(r.resultAsString());
                tx.success();
            }
        }finally {
            db.shutdown();
        }
    }

    @Test
    public void rangeTest(){
        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( new File("Z:\\TEMP") );
        try{
            try(Transaction tx = db.beginTx()){
                int startTime = Helper.timeStr2int("201005020800");
                int endTime =  Helper.timeStr2int("201005021200");
                System.out.println(startTime);
                System.out.println(endTime);
                int i = 0;
                for(Relationship r: GlobalGraphOperations.at(db).getAllRelationships()){
//                    if(i++>100) break;
//                    PackInternalKeyIterator.cnt = 0;
                    Object cnt = r.getTemporalProperty("travel_time", new TimePoint(startTime), new TimePoint(endTime), new TemporalRangeQuery() {
                        int cnt = 0;
                        @Override
                        public boolean onNewEntry(long entityId, int propertyId, TimePointL time, Object val) {
//                            System.out.print(time.val()+" ");
                            cnt++;
                            return true;
                        }
                        @Override
                        public Object onReturn() {
                            return cnt;
                        }
                    });
//                    System.out.println(cnt);
//                    System.out.println(PackInternalKeyIterator.cnt);
//                    System.out.println("-------------------------------");
//                    int count = 0;
//                    int cur = startTime;
//                    while(cur < endTime){
//                        Object obj = r.getTemporalProperty("travel_time", new TimePoint(cur));
//                        if(obj!=null) count++;
//                        cur+=300;
//                    }
//                    System.out.println(count);
                }
                tx.success();
            }
        }finally {
            db.shutdown();
        }
    }

    @Test
    public void calcDataLineCount(){
        List<File> fileList = Helper.trafficFileList("D:\\bygzip", "0501", "0630");
        TrafficMultiFileReader r = new TrafficMultiFileReader(fileList);
        long i = 0;
        while(r.hasNext()){
            i++;
            r.next();
        }
        System.out.println(i);
    }
}
