package org.act.temporal.test.multiThread;

import org.act.neo4j.temporal.demo.Config;
import org.act.temporal.test.multiThread.clients.ConsumerWriteClient;
import org.act.temporal.test.multiThread.clients.ReadFileServer;
import org.act.temporal.test.utils.DataImportor;
import org.act.temporal.test.utils.Helper;
import org.act.temporal.test.vo.Line;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by song on 16-2-24.
 */
public class WriteTest {

    private static Config config = new Config();
    private static List<File> fileList = new ArrayList<>();
    private static GraphDatabaseService db;

    private int writeThreadCount=32;
    private float percent = 1f;
    private List<ConsumerWriteClient> clientList = new ArrayList<>();
    private BlockingQueue<Line> travel_time_queue = new ArrayBlockingQueue<>(200000000);
    private BlockingQueue<Line> full_status_queue = new ArrayBlockingQueue<>(200000000);
    private BlockingQueue<Line> vehicle_count_queue = new ArrayBlockingQueue<>(200000000);
    private BlockingQueue<Line> segment_count_queue = new ArrayBlockingQueue<>(200000000);


    @BeforeClass
    public static void prepareFileList() throws IOException {
        config.dbPath += "-write";
        Helper.deleteExistDB(config);
        db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(config.dbPath)
                .loadPropertiesFromFile("")
                .newGraphDatabase();
        DataImportor.importNetwork(db);
        Helper.getFileRecursive(new File(config.dataPathDir), fileList, 5);
        fileList.sort(null);
//        Collections.shuffle(fileList);
        db.shutdown();
    }

    @Before
    public void prepareClient() throws FileNotFoundException {
        config.db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(config.dbPath)
                .loadPropertiesFromFile("")
                .newGraphDatabase();
//        for(int i=0;i<writeThreadCount;i++){
        clientList.add(new ConsumerWriteClient(config, travel_time_queue, "travel-time"));
        clientList.add(new ConsumerWriteClient(config, full_status_queue, "full-status"));
        clientList.add(new ConsumerWriteClient(config, vehicle_count_queue, "vehicle-count"));
        clientList.add(new ConsumerWriteClient(config, segment_count_queue, "segment-count"));
//        }
    }

    @Test
    public void concurrentTest() throws InterruptedException {
        ReadFileServer server =
                new ReadFileServer(
                        fileList.subList(0, (int) (percent*(fileList.size()-1))),
                        full_status_queue,
                        travel_time_queue,
                        segment_count_queue,
                        vehicle_count_queue);
        server.start();
        for(ConsumerWriteClient client:clientList){
            client.start();
        }
        server.join();
        config.logger.info("file read finished.");
//        for(ConsumerWriteClient client:clientList){
//            client.exit=true;
//        }
        for(ConsumerWriteClient client:clientList){
            client.join();
        }
    }

    @After
    public void shutdown(){
        db.shutdown();
    }



}
