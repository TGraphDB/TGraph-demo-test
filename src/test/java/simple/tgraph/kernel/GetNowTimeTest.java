//package simple.tgraph.kernel;
//
//
//import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
//import edu.buaa.benchmark.client.DBProxy;
//import edu.buaa.benchmark.client.TGraphExecutorClient;
//import edu.buaa.benchmark.transaction.SnapshotAggrDurationTx;
//import edu.buaa.utils.Helper;
//import org.act.temporalProperty.query.TimePointL;
//import org.apache.commons.lang3.tuple.Triple;
//import org.junit.After;
//import org.junit.Test;
//import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.graphdb.Relationship;
//import org.neo4j.graphdb.Transaction;
//import org.neo4j.graphdb.factory.GraphDatabaseFactory;
//import org.neo4j.temporal.TemporalRangeQuery;
//import org.neo4j.tooling.GlobalGraphOperations;
//
//
//import java.io.File;
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.concurrent.ExecutionException;
//
//public class GetNowTimeTest {
//    GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File("E:\\TEST_DB\\tx1\\test-db"));
//@Test
//public void getNowTimeValue() throws Exception{
//        try (Transaction t = db.beginTx()) {
//            for (Relationship r : GlobalGraphOperations.at(db).getAllRelationships()) {
//                Object v = r.getTemporalProperty("jam_status", Helper.time(Helper.timeStr2int("201005010940")), Helper.time(Helper.timeStr2int("201105010940")), new TemporalRangeQuery() {
//                    List<TimePointL> timeList = new ArrayList<>();
//                    @Override
//                    public void onNewEntry(long entityId, int propertyId, TimePointL time, Object val) {
//                        timeList.add(time);
//                    }
//
//                    @Override
//                    public Object onReturn() {
//                        long nowTimeValue = timeList.get(timeList.size()-1).val();
//                        Date date = new Date(nowTimeValue*1000);
//                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                        String nowTimeString = format.format(nowTimeValue);
//                        System.out.println("Now Time: " +nowTimeString);
//
//                        return 0;
//                    }
//                });
//            }
//        }
//    }
//
//    @After
//    public void close() throws IOException, InterruptedException{
//        db.shutdown();
//    }
//}
