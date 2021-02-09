//package simple.tgraph.kernel;
//
//import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
//import edu.buaa.benchmark.client.DBProxy;
//import edu.buaa.benchmark.client.TGraphExecutorClient;
//import edu.buaa.benchmark.transaction.AbstractTransaction;
//import edu.buaa.benchmark.transaction.EntityTemporalConditionTx;
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
//public class CodeTest {
//    GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File("E:\\tgraph\\test-db"));
//
//    @Test
//    public void algoTest() throws Exception {
//        try (Transaction t = db.beginTx()) {
//            List<String> answers = new ArrayList<>();
//            final String[] eligibleResult = new String[1];
//            for (Relationship r : GlobalGraphOperations.at(db).getAllRelationships()) {
//                String roadName = (String) r.getProperty("name");
//                Object v = r.getTemporalProperty("travel_time", Helper.time(Helper.timeStr2int("201006300830")), Helper.time(Helper.timeStr2int("201006300930")), new TemporalRangeQuery() {
//                    @Override
//                    public void onNewEntry(long entityId, int propertyId, TimePointL time, Object val) {
//                        if ((Integer) val > 600) {
//                            eligibleResult[0] = roadName;
//                        }
//                    }
//
//                    @Override
//                    public Object onReturn() {
//                        return eligibleResult[0];
//                    }
//                });
//                answers.add(eligibleResult[0]);
//            }
//            EntityTemporalConditionTx.Result result = new EntityTemporalConditionTx.Result();
//            result.setRoads(answers);
//        }
//
//    }
//}
