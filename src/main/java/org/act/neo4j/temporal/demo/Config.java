package org.act.neo4j.temporal.demo;

import org.act.neo4j.temporal.demo.driver.OperationProxy;
import org.act.neo4j.temporal.demo.driver.real.Neo4jTemporalProxy;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        if (System.getProperty("os.name").toLowerCase().contains("windows")){
            dataPathNetwork = "D:\\TGraph\\demo\\roaddata\\data\\traffic-data-demo\\Topo.csv";
            dataPathDir = "D:\\TGraph\\demo\\roaddata\\data\\traffic-data-demo";
            dataPathFile = "D:\\TGraph\\demo\\roaddata\\data\\traffic-data-demo";
            dbPath = "D:\\TGraph\\demo\\runtime\\test";
        }else{
            if(System.getProperty("user.name").equals("root")){
                dataPathNetwork = "/mnt/Topo.csv";
                dataPathDir = "/mnt/neo4jtest";
                dataPathFile = "/mnt/temporal.data";
                dbPath = "/mnt/neo4j-temporal-test";
            }else {
                dataPathNetwork = "/home/song/project/going/neo4j/doc/traffic-data-demo/Topo.csv";
                dataPathDir = "/home/song/project/going/neo4j/doc/traffic-data-demo/";
                dataPathFile = "/home/song/project/going/neo4j/doc/traffic-data-demo/temporal.data";
                dbPath = "/home/song/tmp/neo4j-temporal-demo-algorithm";
            }
        }
//        neo4jConfigFile = new File(Config.class.getResource("neo4j.config").toURI()).getAbsolutePath();
        logger = LoggerFactory.getLogger("");
    }

    public static Config Default = new Config();



}
