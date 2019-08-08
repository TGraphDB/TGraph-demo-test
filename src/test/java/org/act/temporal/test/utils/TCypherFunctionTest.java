package org.act.temporal.test.utils;

import com.aliyun.openservices.aliyun.log.producer.LogProducer;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.ProducerConfig;
import com.aliyun.openservices.aliyun.log.producer.ProjectConfig;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.aliyun.openservices.log.common.LogItem;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.IndexDefinition;

/**
 * Created by song on 2018-07-26.
 */
public class TCypherFunctionTest
{
    @Test
    public void basicCypherTest() throws IOException
    {
        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( new File( System.getProperty( "java.io.tmpdir" ), "TGRAPH-db" ) );
        try (Transaction tx = db.beginTx()){
            for (Node node: db.getAllNodes()){
                node.delete();
            }
            tx.success();
        }

        try ( Transaction tx = db.beginTx())
        {
            Node myNode = db.createNode();
            myNode.setProperty( "name", "my node" );
            // supported queries:
            // CREATE ({name:'my node',tp:TV(1~4:16, 5~8:9, 12~2010:3)})
            // match (n{name:'my node'}) set n.tp = TV(1~4:16, 5~8:9,12~2010:3)
            db.execute( "match (n{name:'my node'}) set n.tp = TV(1~4:16, 5~8:9, 12~2010:3)" );//
            myNode.setTemporalProperty( "hehe", 1, 30, "A" );
            myNode.setTemporalProperty( "haha", 1, 30, 1 );
            tx.success();
        }

        try (Transaction tx = db.beginTx()){
            for (Node node: db.getAllNodes()){
                node.setTemporalProperty( "haha", 20, 44, 2 );
                Object tv1 = node.getTemporalProperty( "tp", 4 );
                Object tv2 = node.getTemporalProperty( "hehe", 24 );
                Object tv3 = node.getTemporalProperty( "haha", 24 );
                Object v = node.getProperty( "name" );
                System.out.println(node.getId()+" "+tv1+" "+tv2+" "+tv3+" "+v);
            }
        }

        try ( Transaction ignored = db.beginTx();
              Result result = db.execute( "match (n) where TemporalContains(n.tp, TV(1~4:16, 5~8:9)) return n.name, n.tp" ) ) //n.name = 'my node' AND
        {
            System.out.println(result.resultAsString());
//            while ( result.hasNext() )
//            {
//                Map<String,Object> row = result.next();
//                for ( Map.Entry<String,Object> column : row.entrySet() )
//                {
//                    System.out.println( column.getKey() + ": " + column.getValue() + "; " );
//                }
//            }
        }
    }

    @Test
    public void writeTest() throws IOException, ProducerException, InterruptedException {
        GraphDatabaseService db = initDB(true);

        //initialize aliyun log recorder/uploader
        ProducerConfig pConf = new ProducerConfig();
        pConf.setIoThreadCount( 1 ); // one thread to upload
        Producer producer = new LogProducer( pConf );
        producer.putProjectConfig(new ProjectConfig("tgraph-demo-test", "cn-beijing.log.aliyuncs.com", "LTAIeA55PGyOpgZs", "H0XzCznABsioSI4TQpwXblH269eBm6"));

        // create node and its temporal property and get node id.
        long nodeId = 0;
        try(Transaction tx = db.beginTx()){
            Node n = db.createNode();
            nodeId = n.getId();
            n.setTemporalProperty("travel_time", 0, 0);
            tx.success();
        }


        // Run cypher write query in transaction and calc tx latency. and log to aliyun cloud.
        int time = 0;
        int NOW = Integer.MAX_VALUE - 4;
        for(int i=0; i<1000; i++){ // test 5 transaction to avg, you can test more, like 50000 to trigger JIT(run faster).
            long txStartTime = System.currentTimeMillis();
            try(Transaction tx = db.beginTx()){
                // commit 4 data line in 1 transaction.
                for(int j=0; j<4; j++) {
                    db.execute("Match (n) WHERE n.id='" + nodeId + "' SET n.travel_time = TV("+time+"~"+NOW+":"+(time+2)+")");
                    time += 2;
                }
                tx.success();
            }

            // log a line after one transaction.

            // create one line of log
            LogItem log = new LogItem();
            // a line of log is a json object, can have many key-value pairs.
            // aliyun log do not have milliseconds, so we must calculate the latency.
            long curTime = System.currentTimeMillis();
            log.PushBack( "tx_complete_time_millisecond", String.valueOf(curTime));
            log.PushBack( "tx_latency_millisecond", String.valueOf(curTime-txStartTime));
            //param 'topic' should be your test topic, and 'source' is the hardware environment id.
            // 'project' and 'logStore' is fixed.
            producer.send("tgraph-demo-test", "tgraph-log", "tmp-test-2019.8.8-v3", "sjh-PC", log);
        }


        try(Transaction tx = db.beginTx()){
            for(int t : Arrays.asList(0, 10, 20, 30, 40)){
                Object o = db.getNodeById(nodeId).getTemporalProperty("travel_time", t);
                System.out.println(o);
            }
            tx.success();
        }

        producer.close(); //flush log to aliyun cloud.
    }

    @Test
    public void cypherIndexTest() throws IOException
    {
        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( new File( System.getProperty( "java.io.tmpdir" ), "TGRAPH-db" ) );
        try (Transaction tx = db.beginTx()){
            for (Node node: db.getAllNodes()){
                node.delete();
            }
            tx.success();
        }
        ProducerConfig pConf = new ProducerConfig();
        pConf.setIoThreadCount( 1 );
        Producer producer = new LogProducer( pConf );
        LogItem log = new LogItem(); // one line of log
        log.PushBack( "", "" );
        try
        {
            producer.send("tgraph-demo-test", "tgraph-log", "tmp-test-2019.7.30", "sjh-PC", log);
            producer.close();
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
        catch ( ProducerException e )
        {
            e.printStackTrace();
        }


        Label label = DynamicLabel.label( "Neo4j" );
        try ( Transaction tx = db.beginTx())
        {
            for(int i=0;i<10;i++)
            {
                Node myNode = db.createNode( label );
                db.temporalIndex().nodeQueryValueIndex( 0, 3 ).propertyValRange( "hehe", 0, 4 ).query();
                myNode.setProperty( "name", "neo4j"+i );
                myNode.setTemporalProperty( "tp", 1, 4, i );
                myNode.setTemporalProperty( "tp", 6, 8, i );
            }
            tx.success();
        }

//        IndexDefinition indexId;
//        try (Transaction tx = db.beginTx())
//        {
//            indexId = db.schema().indexFor( label ).on( "name" ).create();
//            tx.success();
//        }

//        try (Transaction tx = db.beginTx())
//        {
//            db.schema().awaitIndexOnline( indexId, 10, TimeUnit.SECONDS );
//            tx.success();
//        }

//        try (Transaction tx = db.beginTx())
//        {
//            try(ResourceIterator<Node> nodes = db.findNodes( label, "name", "neo4j4" )){
//                while(nodes.hasNext()){
//                    Node n = nodes.next();
//                    System.out.println(n.getProperty( "name" ));
//                }
//            }
//            tx.success();
//        }

        try ( Transaction tx = db.beginTx();
              Result result = db.execute( "CREATE TEMPORAL MinMax INDEX ON (tp) DURING 2 ~ 8 " ))
        {
            System.out.println(result.resultAsString());
            tx.success();
        }

        try ( Transaction ignored = db.beginTx();
              Result result = db.execute( "MATCH (n:Neo4j) WHERE n.tp ~= TV(1~4:5) RETURN n" ))
        {
            System.out.println(result.resultAsString());
        }
    }


    @Test
    public void cypherAggrTest() throws IOException
    {
        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( new File( System.getProperty( "java.io.tmpdir" ), "TGRAPH-db" ) );
        try (Transaction tx = db.beginTx()){
            for (Node node: db.getAllNodes()){
                node.delete();
            }
            tx.success();
        }

        Label label = DynamicLabel.label( "Neo4j" );
        try ( Transaction tx = db.beginTx())
        {
            for(int i=0;i<10;i++)
            {
                Node myNode = db.createNode( label );
                myNode.setProperty( "name", "neo4j"+i );
                myNode.setTemporalProperty( "tp", 1, 4, i );
                myNode.setTemporalProperty( "tp", 6, 8, i );
            }
            tx.success();
        }

        try ( Transaction ignored = db.beginTx();
              Result result = db.execute( "MATCH (n:Neo4j) WHERE n.tp ~= TV(1~4:5) RETURN TAGGRMIN(n.tp, 10, 20)" ))
        {
            System.out.println(result.resultAsString());
        }
    }


    private static void deleteFile(File element ) {
        if (element.isDirectory()) {
            for (File sub : element.listFiles()) {
                deleteFile(sub);
            }
        }
        element.delete();
    }


    private GraphDatabaseService initDB(boolean fromScratch ) throws IOException
    {
        File dir = new File( System.getProperty( "java.io.tmpdir" ), "TGRAPH-db" );
        if ( fromScratch )
        {
            deleteFile( dir );
        }
        return new GraphDatabaseFactory().newEmbeddedDatabase( dir );
    }
}
