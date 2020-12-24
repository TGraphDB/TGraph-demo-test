package simple.tgraph.kernel.index;

import com.google.common.base.Preconditions;
import edu.buaa.benchmark.BenchmarkTxGenerator;
import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.benchmark.transaction.ImportStaticDataTx;
import edu.buaa.benchmark.transaction.ImportTemporalDataTx;
import edu.buaa.model.StatusUpdate;
import edu.buaa.model.TrafficTemporalPropertyGraph;
import edu.buaa.server.RoadType;
import edu.buaa.utils.Helper;
import org.act.temporalProperty.index.IndexType;
import org.act.temporalProperty.query.TimePointL;
import org.act.temporalProperty.query.aggr.AggregationIndexQueryResult;
import org.act.temporalProperty.util.Slice;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.temporal.TemporalRangeQuery;
import org.neo4j.temporal.TimePoint;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 先用几天的数据进行测试
 */
public class SimpleIndexTest {
    private static GraphDatabaseService db;
    private static Map<String, Long> roadMap;

    @BeforeClass
    public static void connectdb(){
        roadMap = new HashMap<>();
        db = new GraphDatabaseFactory().newEmbeddedDatabase( new File("Z:\\TEMP") );
    }

    private void execute(ImportStaticDataTx tx){
        try(Transaction t = db.beginTx()) {
            for (ImportStaticDataTx.StaticCrossNode p : tx.getCrosses()) {
                Node n = db.createNode();
                n.setProperty("name", p.getName());
                Preconditions.checkArgument(n.getId()==p.getId(), "id not match!");
            }
            for (ImportStaticDataTx.StaticRoadRel sr : tx.getRoads()) {
                Node start = db.getNodeById(sr.getStartCrossId());
                Node end = db.getNodeById(sr.getEndCrossId());
                Relationship r = start.createRelationshipTo(end, RoadType.ROAD_TO);
                r.setProperty("name", sr.getId());
                Preconditions.checkArgument(r.getId()==sr.getRoadId(), "id not match");
                roadMap.put(sr.getId(), r.getId());
            }

            t.success();
        }
    }

    private void execute(ImportTemporalDataTx tx) {
        try(Transaction t = db.beginTx()) {
            for(StatusUpdate s : tx.data){
                Relationship r = db.getRelationshipById(roadMap.get(s.getRoadId()));
                TimePoint time = Helper.time(s.getTime());
                r.setTemporalProperty("travel_time", time, s.getTravelTime());
                r.setTemporalProperty("full_status", time, s.getJamStatus());
                r.setTemporalProperty("segment_count", time, s.getSegmentCount());
            }
            t.success();
        }
    }

    @Test
    public void initdb() throws IOException {
        // static import
        String dataFilePath = "D:\\bygzip";
        TrafficTemporalPropertyGraph tgraph = new TrafficTemporalPropertyGraph();
        tgraph.importTopology(new File(dataFilePath, "road_topology.csv.gz"));
        execute(BenchmarkTxGenerator.txImportStatic(tgraph));
        // create index
        
        // import temporal data.
        List<File> fileList = Helper.trafficFileList(dataFilePath, "0501", "0502");
//        System.out.println(fileList.size());
        try(BenchmarkTxGenerator.TemporalPropertyAppendTxGenerator g = new BenchmarkTxGenerator.TemporalPropertyAppendTxGenerator(10000, fileList)) {
            int i=0;
            while (g.hasNext()) {
                AbstractTransaction tx = g.next();
                execute((ImportTemporalDataTx) tx);
                if(++i==100) createAggrMinMaxIndex();
            }
        }
    }

//    @Test
//    public void statistic(){
//        try(Transaction tx = db.beginTx()){
//            long maxTime = -1;
//            for(Relationship r : GlobalGraphOperations.at(db).getAllRelationships()){
//                r.getTemporalProperty()
//            }
//        }
//    }

    @Test
    public void createEntityTemporalValueIndex(){

    }

    @Test
    public void createAggrMinMaxIndex(){
        try(Transaction tx = db.beginTx()){
            long indexId = db.temporalIndex().relCreateMinMaxIndex(
                    Helper.time(Helper.timeStr2int("201005020800")),
                    Helper.time(Helper.timeStr2int("201005021200")),
                    "travel_time", 1, Calendar.HOUR, IndexType.AGGR_MAX);
            tx.success();
            System.out.println(indexId);
        }
    }

    @Test
    public void queryAggrMinMaxIndex(){
        long indexId = 0;
        Map<Long, Integer> resultMap = new HashMap<>();
        int cnt = 0;
        try(Transaction tx = db.beginTx()){
             for(Relationship r : GlobalGraphOperations.at(db).getAllRelationships()){
                 if(cnt++ > 1000) break;
                 AggregationIndexQueryResult val = r.getTemporalPropertyWithIndex("travel_time", Helper.time(Helper.timeStr2int("201005020800")), Helper.time(Helper.timeStr2int("201005021200")), indexId);
                 Slice result = val.getMinMaxResult().get(0);
                 if(result!=null) resultMap.put(r.getId(), result.getInt(0));
             }
            tx.success();
            System.out.println(resultMap.size());
        }
    }

    @Test
    public void queryAggrMinMaxWithoutIndex(){
        Map<Long, Integer> resultMap = new HashMap<>();
        try(Transaction tx = db.beginTx()){
            int cnt = 0;
            for(Relationship r : GlobalGraphOperations.at(db).getAllRelationships()){
                if(cnt++ > 200) break;
                Object result = r.getTemporalProperty("travel_time", Helper.time(Helper.timeStr2int("201005020800")), Helper.time(Helper.timeStr2int("201005021200")), new TemporalRangeQuery() {
                    int max = -1;
                    @Override
                    public void onNewEntry(long entityId, int propertyId, TimePointL time, Object val) {
                        if (val != null) {
                            int vInt = (Integer) val;
                            if (vInt > max) max = vInt;
                        }
                    }
                    @Override
                    public Object onReturn() {
                        return max;
                    }
                });
                if(result!=null && ((Integer)result)!=-1) resultMap.put(r.getId(), (Integer) result);
            }
            tx.success();
            System.out.println(resultMap.size());
        }
    }


    @AfterClass
    public static void closedb(){
        db.shutdown();
    }
}
