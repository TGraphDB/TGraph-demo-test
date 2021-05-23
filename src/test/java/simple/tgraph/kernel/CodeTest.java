package simple.tgraph.kernel;

import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.google.common.base.Preconditions;
import edu.buaa.algo.EarliestArriveTime;
import edu.buaa.benchmark.client.DBProxy;
import edu.buaa.benchmark.client.TGraphExecutorClient;
import edu.buaa.benchmark.transaction.*;
import edu.buaa.server.RoadType;
import edu.buaa.server.TGraphKernelTcpServer;
import edu.buaa.utils.Helper;
import org.act.temporalProperty.index.value.IndexMetaData;
import org.act.temporalProperty.query.TimePointL;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.After;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.temporal.TemporalRangeQuery;
import org.neo4j.tooling.GlobalGraphOperations;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class CodeTest {
    GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File("C:\\tgraph\\test-db\\Tgraph-bj60c-49day"));

    @Test
    public void algoTest() throws Exception {
        try(Transaction t = db.beginTx()) {

            EarliestArriveTime algo = new TGraphKernelTcpServer.EarliestArriveTimeTGraphKernel(db, "travel_time", 47294, Helper.timeStr2int("201005300830"), 3600);
            List<EarliestArriveTime.NodeCross> answers = new ArrayList<>(algo.run());
            answers.sort(Comparator.comparingLong(EarliestArriveTime.NodeCross::getId));
            ReachableAreaQueryTx.Result result = new ReachableAreaQueryTx.Result();
            result.setNodeArriveTime(answers);
            System.out.println(answers);
//            t.failure();//do not commit;

            t.success();
        }

        finally {
            db.shutdown();
        }

    }
}
