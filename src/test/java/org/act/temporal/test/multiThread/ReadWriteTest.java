package org.act.temporal.test.multiThread;

import org.act.tgraph.demo.Config;
import org.act.temporal.test.multiThread.clients.Client;
import org.act.temporal.test.multiThread.clients.ReadPointClient;
import org.act.temporal.test.multiThread.clients.ReadRangeAggregateClient;
import org.act.temporal.test.multiThread.clients.WriteOneLineClient;
import org.act.temporal.test.utils.DataImportor;
import org.act.temporal.test.utils.Helper;
import org.act.temporal.test.utils.Monitor;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.*;
import java.util.*;
/**
 * Created by song on 16-2-25.
 */
public class ReadWriteTest {
    private static List<File> fileList = new ArrayList<>();
    private static List<Integer> timeList=new ArrayList<>();
    private static GraphDatabaseService db;
    private static Config config= new Config();

    private int totalThreadCount = 32;
    private float readThreadPercent = 0.1f;
    private float dataPercent=0.2f;

    private List<Client> readClientList = new ArrayList<>();
    private List<Client> writeClientList = new ArrayList<>();
    private List<Monitor.SnapShot> log = Collections.synchronizedList(new ArrayList<>());


    @BeforeClass
    public static void prepareFileList() throws IOException {
        config.dbPath += "-read-write";
        Helper.deleteExistDB(config);
        db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(new File(config.dbPath))
                .loadPropertiesFromFile("")
                .newGraphDatabase();
        DataImportor.importNetwork(db);
        Helper.getFileRecursive(new File(config.dataPathDir), fileList, 5);
        fileList.sort(null);
        Collections.shuffle(fileList);
        for(File file:fileList){
            timeList.add(Helper.getFileTime(file));
        }
        db.shutdown();
    }

    @Before
    public void prepareClient() throws FileNotFoundException {
        db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(new File(config.dbPath))
                .loadPropertiesFromFile("")
                .newGraphDatabase();
        for(int i=0;i< totalThreadCount;i++){
            if(i<readThreadPercent*totalThreadCount){
                if(i%2==0) {
                    readClientList.add(new ReadPointClient(config,timeList));
                }else{
                    readClientList.add(new ReadRangeAggregateClient(config,timeList));
                }
            }else{
                int[] tmp = Helper.calcSplit(1, totalThreadCount, (int) (fileList.size()* dataPercent));
                int from = tmp[0];
                int to = tmp[1];
                writeClientList.add(new WriteOneLineClient(config, fileList.subList(from, to+1), db ));
            }
        }
    }

    @Test
    public void concurrentTest() throws InterruptedException, IOException {
        for(Client client:writeClientList){
            client.start();
        }
        for(Client client:readClientList){
            client.start();
        }
        for(Client client:writeClientList){
            client.join();
        }
        for(Client client:readClientList) {
            client.exit = true;
        }
        for(Client client:readClientList){
            client.join();
            client.logToFile();
        }
        for(Client client:writeClientList){
            client.logToFile();
        }
    }

    @After
    public void shutdown(){

        db.shutdown();
    }














}
