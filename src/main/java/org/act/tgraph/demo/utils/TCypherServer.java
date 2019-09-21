package org.act.tgraph.demo.utils;


import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.sun.management.OperatingSystemMXBean;
import org.act.tgraph.demo.Config;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class TCypherServer {
    private final String dbPath;

    private final Producer logger;
    private GraphDatabaseService db;
    private volatile boolean shouldRun = true;
    private ServerSocket server;
    private final List<Thread> threads = Collections.synchronizedList(new LinkedList<>());
    private final String serverCodeVersion;

    public static void main(String[] args){
        TCypherServer server = new TCypherServer(Config.Default.onlineLogger, args[0]);
//        TCypherServer server = new TCypherServer("AMITABHA", Config.Default.onlineLogger, "/media/song/test/db-network-only");
        try {
            server.start();
        } catch (IOException | InterruptedException | ProducerException e) {
            e.printStackTrace();
        }
    }

    public TCypherServer(Producer logger, String dbPath) {
        this.logger = logger;
        this.dbPath = dbPath;
        serverCodeVersion = Config.Default.gitStatus;
        System.out.println("server code version: "+ serverCodeVersion);
    }




    public void start() throws IOException, InterruptedException, ProducerException {
        db = new GraphDatabaseFactory().newEmbeddedDatabase( new File(dbPath));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            db.shutdown();
//            try {
//                logger.close();
//            } catch (InterruptedException | ProducerException e) {
//                e.printStackTrace();
//            }
        }));
        MonitorThread monitor = new MonitorThread();
        monitor.start();

        server = new ServerSocket(8438);
        System.out.println("waiting for client to connect.");

        while(shouldRun) {
            Socket client;
            try {
                client = server.accept();
            }catch (SocketException ignore){ // closed from another thread.
                break;
            }
            Thread t = new ServerThread(client, monitor);
            threads.add(t);
            System.out.println("GET one more client, currently "+threads.size()+" client");
            t.setDaemon(true);
            t.start();
        }
        for(Thread t : threads){
            t.join();
        }
        db.shutdown();
        logger.close();
        System.out.println("main thread exit.");
    }


    private class Req implements Runnable{
        String[] queries;
        String[] results = new String[0];
        int resultSize;
        boolean success=true;

        @Override
        public void run() {
            results = new String[queries.length];
            try {
                try (Transaction tx = db.beginTx()) {
                    for (int i = 0; i < queries.length; i++) {
                        String query = queries[i];
                        Result result = db.execute(query);
                        results[i] = result.resultAsString().replace("\n", "\\n").replace("\r", "\\r");
                        resultSize += results[i].length();
                    }
                    tx.success();
                }
            }catch (Exception msg){
                msg.printStackTrace();
                success = false;
            }
        }
    }

    private class MonitorThread extends Thread{
        volatile ServerStatus serverStatus;

        public void run(){
            final OperatingSystemMXBean bean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            Runtime runtime = Runtime.getRuntime();
            try {
                long lastTime = System.currentTimeMillis();
                long disksWrite = 0, disksRead=0;
                while(shouldRun){
                    Thread.sleep(1_000);
                    long curDisksWrite = 0, curDisksRead=0, curDiskQueueLen=0;
                    HWDiskStore[] disks = new SystemInfo().getHardware().getDiskStores();
                    for(HWDiskStore disk : disks){
                        curDisksWrite += disk.getWriteBytes();
                        curDisksRead += disk.getReadBytes();
                        curDiskQueueLen += disk.getCurrentQueueLength();
                    }
                    long now = System.currentTimeMillis();
                    ServerStatus s = new ServerStatus();
                    s.activeConn = threads.size();
                    s.curMem = runtime.totalMemory() - runtime.freeMemory();
                    s.processCpuLoad = bean.getProcessCpuLoad();
                    s.systemCpuLoad = bean.getSystemCpuLoad();
                    s.diskWriteSpeed = (curDisksWrite - disksWrite)/(now-lastTime)*1000;
                    s.diskReadSpeed = (curDisksRead - disksRead)/(now-lastTime)*1000;
                    s.diskQueueLength = curDiskQueueLen;
                    this.serverStatus = s;
                    disksRead = curDisksRead;
                    disksWrite = curDisksWrite;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class ServerStatus{
        long time = System.currentTimeMillis();
        int  activeConn;
        long curMem;
        long diskReadSpeed;
        long diskWriteSpeed;
        long diskQueueLength;
        double processCpuLoad;
        double systemCpuLoad;
    }

    private class ServerThread extends Thread{
        private final MonitorThread monitor;
        Socket client;
        BufferedReader fromClient;
        PrintStream toClient;
        long reqCnt = 0;

        ServerThread(Socket client, MonitorThread monitor) throws IOException {
            this.client = client;
            this.monitor = monitor;
            client.setTcpNoDelay(true);
            this.fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            this.toClient = new PrintStream(client.getOutputStream(), true);
        }

        public void run(){
            long tid = Thread.currentThread().getId();
            Thread.currentThread().setName("TCypher con "+tid);
            System.out.println(Thread.currentThread().getName()+" started.");
            TimeMonitor time = new TimeMonitor();
            time.begin("Send");
            try {
                while(true){
                    time.mark("Send", "Wait");
                    long previousSendTime = time.duration("Send");
                    String line;
                    try {
                        line = fromClient.readLine();
                    }catch (SocketException ignore){// client close conn.
                        System.out.println("client close connection.");
                        client.close();
                        break;
                    }
                    if(line==null){
                        System.out.println("client close connection. read end.");
                        client.close();
                        break;
                    }else if("EXIT".equals(line)){ //client ask server exit;
                        client.close();
                        server.close();
                        shouldRun = false;
                        System.out.println("client ask server exit.");
                        break;
                    }else if("GC".equals(line)){
                        Runtime.getRuntime().gc();
                        System.out.println("client ask server gc.");
                        continue;
                    }else if(line.startsWith("TOPIC:")){
                        String testTopic = line.substring(6);
                        System.out.println("topic changed to "+ testTopic);
                        toClient.println("Server code version:"+serverCodeVersion);
                        continue;
                    }
                    time.mark("Wait", "Transaction");
                    Req req = new Req();
                    req.queries = line.split(";");
                    req.run();
                    time.mark("Transaction", "Send");
                    String result = generateResult(
                            req,
                            time.endT("Wait"),
                            previousSendTime,
                            time.duration("Transaction"),
                            monitor.serverStatus);
                    toClient.println(result);
                    reqCnt++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            threads.remove(this);
            System.out.println(Thread.currentThread().getName()+" exit. process "+reqCnt+" queries.");
        }

        private String generateResult(Req req, long reqGotTime, long previousSendTime, long txTime, ServerStatus s) {
            JsonObject obj = new JsonObject();
            obj.add("success", req.success);
            obj.add("resultSize", req.resultSize);
            JsonArray results = new JsonArray();
            for(String result : req.results){
                results.add(result);
            }
            obj.add("results", results);

            obj.add("t_ReqGot", reqGotTime);
            obj.add("t_PreSend", previousSendTime);
            obj.add("t_Tx", txTime);

            obj.add("s_updateTime", s.time);
            obj.add("s_memory", s.curMem);
            obj.add("s_connCnt", s.activeConn);
            obj.add("s_pCPU", s.processCpuLoad);
            obj.add("s_CPU", s.systemCpuLoad);
            obj.add("s_disk_qLen", s.diskQueueLength);
            obj.add("s_disk_read", s.diskReadSpeed);
            obj.add("s_disk_write", s.diskWriteSpeed);
            return obj.toString();
        }
    }






    }
