package org.act.temporal.test.utils;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
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
public class Neo4jFunctionTest
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
    public void cypherIndexTest() throws IOException
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
              Result result = db.execute( "MATCH (n:Neo4j) WHERE n.tp ~= TV(1~4:5) RETURN TAGGRMIN(n.tp, 1, 20)" ))
        {
            System.out.println(result.resultAsString());
        }
    }
}
