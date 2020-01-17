//package org.act.temporal.test.multiThread.clients;
//
//import org.act.tgraph.demo.benchmark.DBOperation;
//import org.act.tgraph.demo.client.Config;
//import org.act.temporal.test.utils.Monitor;
//import org.neo4j.graphdb.GraphDatabaseService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Created by song on 16-2-26.
// */
//public class Client extends Thread{
//    protected long id;
//    protected String type;
//    protected DBOperation proxy;
//    protected GraphDatabaseService db;
//    protected List<Monitor.SnapShot> log;
//    protected Monitor monitor;
//    public boolean exit=false;
//
//    public Client(Config config){
//        log = new ArrayList<>();
//        Logger logger = LoggerFactory.getLogger("Client");
//        monitor = new Monitor(logger);
//        this.db = config.db;
//    }
//
//    public void logToFile() throws IOException {
//        try(BufferedWriter writer = new BufferedWriter(new FileWriter(id+"_"+type+".log"),8192)){
//            for(Monitor.SnapShot shot:log) {
//                writer.append(shot.toString());
//                writer.newLine();
//            }
//        }
//    }
//
//}