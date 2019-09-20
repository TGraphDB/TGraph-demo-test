package org.act.tgraph.demo;

import com.aliyun.openservices.aliyun.log.producer.LogProducer;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.ProducerConfig;
import com.aliyun.openservices.aliyun.log.producer.ProjectConfig;
import org.act.tgraph.demo.driver.OperationProxy;
import org.act.tgraph.demo.driver.real.Neo4jTemporalProxy;
import org.neo4j.graphdb.GraphDatabaseService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

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
    public final Producer onlineLogger = getLogger();
    public final String gitStatus = currentGitStatus();


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
        SystemInfo info = new SystemInfo();
//        info.getOperatingSystem().
    }

    public static Config Default = new Config();

    private static Producer getLogger(){
        ProducerConfig pConf = new ProducerConfig();
        pConf.setIoThreadCount( 1 ); // one thread to upload
        Producer producer = new LogProducer( pConf );
        producer.putProjectConfig(new ProjectConfig("tgraph-demo-test", "cn-beijing.log.aliyuncs.com", "LTAIeA55PGyOpgZs", "H0XzCznABsioSI4TQpwXblH269eBm6"));
        return producer;
    }

    private static String currentGitStatus() {
        try (InputStream input = Config.class.getResourceAsStream("/git.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty("git.commit.id.describe-short");
        } catch (IOException ex) {
            if(ex instanceof FileNotFoundException) return "NoGit";
            ex.printStackTrace();
            return "Git-Err";
        }
    }

}
