package org.act.tgraph.demo;

import org.act.tgraph.demo.driver.OperationProxy;
import org.act.tgraph.demo.driver.real.Neo4jTemporalProxy;
import org.neo4j.graphdb.GraphDatabaseService;

import org.act.tgraph.demo.utils.DataDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by song on 16-2-24.
 */
public class Config {

    public GraphDatabaseService db;
    public OperationProxy proxy = new Neo4jTemporalProxy();
    public String logFileDir = "log";

    public String neo4jConfigFile= "neo4j.config";
    public String dataPathNetwork;
    public String dataPathDir;
    public String dataPathFile;
    public String dbPath;
    public final Logger logger;


    public Config(){
        File tmpDir = new File( System.getProperty( "java.io.tmpdir" ));
        File dataDir = new File(tmpDir, "traffic-data");
        File dbDir = new File(tmpDir, "tgraph-db");
        dataPathDir = dataDir.getAbsolutePath();
        try{
            if ( !dataDir.exists() ) Files.createDirectory(dataDir.toPath());
//            dataPathNetwork = DataDownloader.getTopo( dataDir );
//            DataDownloader.getTrafficData(dataPathDir);
//
            if ( !dbDir.exists() ) Files.createDirectory(dbDir.toPath());
        }catch ( IOException e ){
            e.printStackTrace();
            throw new RuntimeException( e );
        }
        dbPath = dbDir.getAbsolutePath();
//        neo4jConfigFile = new File(Config.class.getResource("neo4j.config").toURI()).getAbsolutePath();
        logger = LoggerFactory.getLogger("");
    }

    public static Config Default = new Config();



}
