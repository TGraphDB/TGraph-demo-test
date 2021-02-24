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
import edu.buaa.utils.Pair;
import org.act.temporalProperty.index.IndexType;
import org.act.temporalProperty.query.TimePointL;
import org.act.temporalProperty.query.aggr.AggregationIndexQueryResult;
import org.act.temporalProperty.util.Slice;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.temporal.IntervalEntry;
import org.neo4j.temporal.TemporalIndexManager;
import org.neo4j.temporal.TemporalRangeQuery;
import org.neo4j.temporal.TimePoint;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 先用几天的数据进行测试
 */
public class SimpleIndexTest {
    private static GraphDatabaseService db;
    private static Map<String, Long> roadMap;

    @BeforeClass
    public static void connectdb(){
        roadMap = new HashMap<>();
        db = new GraphDatabaseFactory().newEmbeddedDatabase( new File("D:\\tgraph\\testdb") );
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

    volatile boolean shouldGo = true;
    @Test
    public void initdb() throws IOException, InterruptedException {
        // static import
        String dataFilePath = "D:\\tgraph\\data";
        TrafficTemporalPropertyGraph tgraph = new TrafficTemporalPropertyGraph();
        tgraph.importTopology(new File(dataFilePath, "road_topology.csv.gz"));
        execute(BenchmarkTxGenerator.txImportStatic(tgraph));
        // create index

        // import temporal data.
        List<File> fileList = Helper.trafficFileList(dataFilePath, "0501", "0502");
//        System.out.println(fileList.size());

        startMonitor();
        try(BenchmarkTxGenerator.TemporalPropertyAppendTxGenerator g = new BenchmarkTxGenerator.TemporalPropertyAppendTxGenerator(10000, fileList)) {
            int i=0;
            while (g.hasNext()) {
                AbstractTransaction tx = g.next();
                execute((ImportTemporalDataTx) tx);
//                if(++i==100) createAggrMinMaxIndex();
            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
        }
        Thread.sleep(10_000);
        shouldGo=false;
    }

    private void startMonitor() {
        Thread t = new Thread(() -> {
            try (Transaction tx = db.beginTx()) {
                Relationship r = db.getRelationshipById(49822); //65556);
                long begin = System.currentTimeMillis();
                int cnt = 0;
                while (shouldGo) {
                    Object result = r.getTemporalProperty("travel_time", new TimePoint(1272772800));
                    System.out.println("\t\t"+(System.currentTimeMillis() - begin) + " " + result + " ");
//                    result = r.getTemporalProperty("travel_time", new TimePoint(1272772801));
//                    System.out.println(result);
                    Thread.sleep(1_000);
                }
                tx.success();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @Test
    public void listTemporalPropertyIndex(){
        try(Transaction tx = db.beginTx()){
            System.out.println(db.temporalIndex().nodeIndexes());
            System.out.println(db.temporalIndex().relIndexes());
            tx.success();
        }
    }

    @Test
    public void dropTemporalPropertyIndex(){
        try(Transaction tx = db.beginTx()){
            db.temporalIndex().relDropIndex(0);
            tx.success();
        }
        try(Transaction tx = db.beginTx()){
            System.out.println(db.temporalIndex().relIndexes());
            tx.success();
        }
    }

    @Test
    public void createAggrMinMaxIndex() throws InterruptedException {
        try(Transaction tx = db.beginTx()){
            long indexId = db.temporalIndex().relCreateMinMaxIndex(
                    Helper.time(Helper.timeStr2int("201005020800")),
                    Helper.time(Helper.timeStr2int("201005021200")),
                    "travel_time", 1, Calendar.HOUR, IndexType.AGGR_MAX);
            tx.success();
            System.out.println(indexId);
        }
        Thread.sleep(10_000);
        listTemporalPropertyIndex();
//        listTemporalPropertyIndex();
//        queryAggrMinMaxIndex();
    }

    Set<Pair<Long, Integer>> resultSet = new HashSet<>();
    @Test
    public void queryAggrMinMaxIndex(){
        long indexId = 228;
        int cnt = 0;
        try(Transaction tx = db.beginTx()){
            for(Relationship r : GlobalGraphOperations.at(db).getAllRelationships()){
                if(cnt++ > 200) break;
                AggregationIndexQueryResult val = r.getTemporalPropertyWithIndex("travel_time",
                        Helper.time(Helper.timeStr2int("201005020800")),
                        Helper.time(Helper.timeStr2int("201005021200")), indexId);
                if(val==null) continue;
                Slice result = val.getMinMaxResult().get(0);
                if(result!=null) resultSet.add(Pair.of(r.getId(), result.getInt(0)));
            }
            tx.success();
            System.out.println(resultSet.size());
        }
    }

    @Test
    public void queryAggrMinMaxWithoutIndex(){
        Set<Pair<Long, Integer>> resultMap = new HashSet<>();
        try(Transaction tx = db.beginTx()){
            int cnt = 0;
            for(Relationship r : GlobalGraphOperations.at(db).getAllRelationships()){
                if(cnt++ > 200) break;
                Object result = r.getTemporalProperty("travel_time", Helper.time(Helper.timeStr2int("201005020800")), Helper.time(Helper.timeStr2int("201005021200")), new TemporalRangeQuery() {
                    int max = -1;
                    @Override
                    public boolean onNewEntry(long entityId, int propertyId, TimePointL time, Object val) {
                        if (val != null) {
                            int vInt = (Integer) val;
                            if (vInt > max) max = vInt;
                        }
                        return true;
                    }
                    @Override
                    public Object onReturn() {
                        return max;
                    }
                });
                if(result!=null && ((Integer)result)!=-1) resultMap.add(Pair.of(r.getId(), (Integer) result));
            }
            tx.success();
            System.out.println(resultMap.size());
        }
        queryAggrMinMaxIndex();
        Helper.compareSets(resultMap, resultSet);
    }


    @Test
    public void createEntityTemporalValueIndex() throws InterruptedException {
        try(Transaction tx = db.beginTx()){
            long indexId = db.temporalIndex().relCreateValueIndex(
                    Helper.time(Helper.timeStr2int("201005020800")),
                    Helper.time(Helper.timeStr2int("201005021200")),
//                    Helper.time(Helper.timeStr2int("201006300800")),
//                    Helper.time(Helper.timeStr2int("201006301200")),
                    "travel_time");
            tx.success();
            System.out.println(indexId);
        }
        Thread.sleep(10_000);
        listTemporalPropertyIndex();
    }

    List<IntervalEntry> results;
    @Test
    public void queryValueIndex(){
        Set<Long> answersFinal;
        try(Transaction tx = db.beginTx()){
            TemporalIndexManager.PropertyValueIntervalBuilder query = db.temporalIndex().relQueryValueIndex(
                    Helper.time(Helper.timeStr2int("201005020800")),
                    Helper.time(Helper.timeStr2int("201005021200"))
//                    Helper.time(Helper.timeStr2int("201006300800")),
//                    Helper.time(Helper.timeStr2int("201006301200"))
            );
            query.propertyValRange("travel_time", 100, 200);
            results = query.query();
            answersFinal = results.stream().map(IntervalEntry::getEntityId).collect(Collectors.toSet());
            tx.success();
        }
        System.out.println(answersFinal.size());
    }

    @Test
    public void queryValueIndexCardinality(){
        try(Transaction tx = db.beginTx()){
            TemporalIndexManager.PropertyValueIntervalBuilder query = db.temporalIndex().relQueryValueIndex(
                    Helper.time(Helper.timeStr2int("201005020800")),
                    Helper.time(Helper.timeStr2int("201005021200"))
//                    Helper.time(Helper.timeStr2int("201006300800")),
//                    Helper.time(Helper.timeStr2int("201006301200"))
            );
            query.propertyValRange("travel_time", 100, 200);
            long answers = query.cardinality();
            System.out.println(answers);
            tx.success();
        }
    }

    @Test
    public void queryValueWithoutIndex(){
        Set<Long> answers = new HashSet<>();
        try(Transaction tx = db.beginTx()){
            for (Relationship r:GlobalGraphOperations.at(db).getAllRelationships()){
                Object v = r.getTemporalProperty("travel_time",
                        Helper.time(Helper.timeStr2int("201005020800")),
                        Helper.time(Helper.timeStr2int("201005021200")),
//                        Helper.time(Helper.timeStr2int("201006300800")),
//                        Helper.time(Helper.timeStr2int("201006301200")),
                        new TemporalRangeQuery() {
                    @Override
                    public boolean onNewEntry(long entityId, int propertyId, TimePointL time, Object val) {
                        int iVal = (Integer) val;
                        if (100 <= iVal && iVal<=200){
                            answers.add(entityId);
                            return false;
                        }
                        return true;
                    }
                    @Override
                    public Object onReturn() {
                        return null;
                    }
                });
            }
            tx.success();
            System.out.println(answers.size());
        }
        queryValueIndex();
        Triple<Set<Long>, Integer, Set<Long>> cpResult = Helper.compareSets(answers, results.stream().map(IntervalEntry::getEntityId).collect(Collectors.toSet()));
        System.out.println(cpResult.getLeft()+"\ncommon.size="+ cpResult.getMiddle()+"\n"+cpResult.getRight());
        validate(cpResult.getLeft());
        validateValueIndex(cpResult.getRight());
    }

    private void validateValueIndex(Set<Long> ids) {
        try(Transaction tx = db.beginTx()) {
            for (Long id : ids) {
                results.stream().filter(intervalEntry -> {
                    Object v = intervalEntry.getVal(0);
                    int val = ((Slice) v).getInt(0);
                    return intervalEntry.getEntityId()==id && 100<=val && val<=200;
                }).forEach(e-> {
                    Object v = e.getVal(0);
                    int val = ((Slice) v).getInt(0);
                    System.out.println(e.getEntityId()+"\t"+e.getStart()+"~"+e.getEnd()+" "+val);
                });
            }
        }
    }

    private void validate(Set<Long> ids){
        try(Transaction tx = db.beginTx()) {
            for (Long id : ids) {
                Relationship r = db.getRelationshipById(id);
                System.out.print(r.getProperty("name"));
                r.getTemporalProperty("travel_time",
                    Helper.time(Helper.timeStr2int("201005020800")),
                    Helper.time(Helper.timeStr2int("201005021200")),
//                        Helper.time(Helper.timeStr2int("201006300800")),
//                        Helper.time(Helper.timeStr2int("201006301200")),
                    new TemporalRangeQuery() {
                        @Override
                        public boolean onNewEntry(long entityId, int propertyId, TimePointL time, Object val) {
                            int iVal = (Integer) val;
                            if (100 <= iVal && iVal<=200){
                                System.out.print(" "+time.val());
                            }
                            return true;
                        }
                        @Override
                        public Object onReturn() {

                            return null;
                        }
                    });
                System.out.println();
            }
            tx.success();
        }
    }


    @Test
    public void valueIndexPerformance(){
        // estimate cardinality only
        try(Transaction tx = db.beginTx()){
            TemporalIndexManager.PropertyValueIntervalBuilder query = db.temporalIndex().relQueryValueIndex(
                    Helper.time(Helper.timeStr2int("201005020800")),
                    Helper.time(Helper.timeStr2int("201005021200"))
            );
            query.propertyValRange("travel_time", 100, 200);
            long beginT = System.currentTimeMillis();
            long answers = 0;
            for(int i=0; i<100; i++) {
                answers = query.cardinality();
            }
            System.out.print(System.currentTimeMillis()-beginT);
            System.out.println(" "+answers);
            tx.success();
        }
        // query index
        try(Transaction tx = db.beginTx()){
            TemporalIndexManager.PropertyValueIntervalBuilder query = db.temporalIndex().relQueryValueIndex(
                    Helper.time(Helper.timeStr2int("201005020800")),
                    Helper.time(Helper.timeStr2int("201005021200"))
            );
            Set<Long> answersFinal = new HashSet<>();
            long beginT = System.currentTimeMillis();
            for(int i=0; i<100; i++) {
                query.propertyValRange("travel_time", 100, 200);
                for(IntervalEntry e : query.query()){
                    answersFinal.add(e.getEntityId());
                }
            }
            System.out.print(System.currentTimeMillis()-beginT);
            System.out.println(" "+answersFinal.size());
            tx.success();
        }
        // query without index
        try(Transaction tx = db.beginTx()){
            Set<Long> answers = new HashSet<>();
            long beginT = System.currentTimeMillis();
            for(int i=0; i<100; i++)
                for (Relationship r:GlobalGraphOperations.at(db).getAllRelationships()){
                    r.getTemporalProperty("travel_time",
                            Helper.time(Helper.timeStr2int("201005020800")),
                            Helper.time(Helper.timeStr2int("201005021200")),
                            new TemporalRangeQuery() {
                                @Override
                                public boolean onNewEntry(long entityId, int propertyId, TimePointL time, Object val) {
                                    int iVal = (Integer) val;
                                    if (100 <= iVal && iVal<=200){
                                        answers.add(entityId);
                                        return false;
                                    }
                                    return true;
                                }
                                @Override
                                public Object onReturn() { return null; }
                            });
                }
            System.out.print(System.currentTimeMillis()-beginT);
            tx.success();
            System.out.println(" "+answers.size());
        }
    }



    @Test
    public void test(){
        try(Transaction tx = db.beginTx()) {
            Relationship r = db.getRelationshipById(49822); //65556);
            System.out.println(r.getProperty("name"));
//            Object result = r.getTemporalProperty("travel_time", new TimePoint(1272772800));
//            System.out.println(result);
//            result = r.getTemporalProperty("travel_time", new TimePoint(1272772801));
//            System.out.println(result);
            r.getTemporalProperty("travel_time",
                    TimePoint.INIT, TimePoint.NOW,
//                    Helper.time(Helper.timeStr2int("201005020800")),
//                    Helper.time(Helper.timeStr2int("201005021200")),
//                        Helper.time(Helper.timeStr2int("201006300800")),
//                        Helper.time(Helper.timeStr2int("201006301200")),
                    new TemporalRangeQuery() {
                        @Override
                        public boolean onNewEntry(long entityId, int propertyId, TimePointL time, Object val) {
                            int iVal = (Integer) val;
//                            if (100 <= iVal && iVal<=200){
                            Date c = new Date(time.val()*1000);
                                System.out.println(c.getHours()+":"+c.getMinutes()+":"+c.getSeconds()+" "+iVal);
//                            }
                            return true;
                        }
                        @Override
                        public Object onReturn() {
                            return null;
                        }
                    });
            tx.success();
        }
//        System.out.println(Helper.time(Helper.timeStr2int("201005020800"))+" "+ Helper.time(Helper.timeStr2int("201005021200")));
    }


    @AfterClass
    public static void closedb(){
        db.shutdown();
    }
}
