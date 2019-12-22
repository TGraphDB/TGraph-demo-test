package org.act.tgraph.demo.client;

import com.aliyun.openservices.aliyun.log.producer.LogProducer;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.ProducerConfig;
import com.aliyun.openservices.aliyun.log.producer.ProjectConfig;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.eclipsesource.json.JsonObject;
import org.act.tgraph.demo.client.driver.OperationProxy;
import org.act.tgraph.demo.client.driver.tgraph.Neo4jTemporalProxy;
import org.neo4j.graphdb.GraphDatabaseService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Created by song on 16-2-24.
 */
public class Config extends JsonObject{
    public GraphDatabaseService db;
    public OperationProxy proxy = new Neo4jTemporalProxy();

    public String dataPathNetwork;
    public String dataPathDir;
    public String dataPathFile;
    public String dbPath;
    public Logger logger;
    private static Producer onlineLogger;
    private static String gitStatus;


    public Config(){
        logger = LoggerFactory.getLogger("");
        onlineLogger = getLogger();
        gitStatus = codeGitVersion();
    }

    private void generateTmpTestDir(){
        File tmpDir = new File( System.getProperty( "java.io.tmpdir" ));
        File dataDir = new File(tmpDir, "traffic-data");
        File dbDir = new File(tmpDir, "tgraph-db");
        dataPathDir = dataDir.getAbsolutePath();
        try{
            if ( !dataDir.exists() ) Files.createDirectory(dataDir.toPath());
            if ( !dbDir.exists() ) Files.createDirectory(dbDir.toPath());
        }catch ( IOException e ){
            e.printStackTrace();
            throw new RuntimeException( e );
        }
        dbPath = dbDir.getAbsolutePath();
//        neo4jConfigFile = new File(Config.class.getResource("neo4j.config").toURI()).getAbsolutePath();
    }

    public Producer getLogger(){
        if(onlineLogger!=null) return onlineLogger;
        ProducerConfig pConf = new ProducerConfig();
        pConf.setIoThreadCount( 1 ); // one thread to upload
        onlineLogger = new LogProducer( pConf );
        onlineLogger.putProjectConfig(new ProjectConfig("tgraph-demo-test", "cn-beijing.log.aliyuncs.com", "LTAIeA55PGyOpgZs", "H0XzCznABsioSI4TQpwXblH269eBm6"));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                onlineLogger.close();
            } catch (InterruptedException | ProducerException e) {
                e.printStackTrace();
            }
        }));
        return onlineLogger;
    }

    public String codeGitVersion() {
        if(gitStatus!=null) return gitStatus;
        try (InputStream input = getClass().getResourceAsStream("/git.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            gitStatus = prop.getProperty("git.commit.id.describe-short");
            if(gitStatus.endsWith("-Modified")){
                gitStatus = gitStatus.replace("-Modified", "(M)");
            }
        } catch (IOException ex) {
            if(ex instanceof FileNotFoundException) return "NoGit";
            ex.printStackTrace();
            gitStatus = "Git-Err";
        }
        return gitStatus;
    }

    public static Config Default = new Config();

    public static Config sjh = new Config();
    static {
        sjh.dbPath = "/media/song/test/db-network-only";
        sjh.add("dir_data_file_by_day", "/media/song/test/data-set/beijing-traffic/TGraph/byday");
        sjh.add("server_host", "192.168.1.245"); //localhost
    }

    public static Config zhangh = new Config();
    static {
        zhangh.dbPath = "E:/tgraph/test-db/db-network-only";
        zhangh.add("", "");

    }
}
