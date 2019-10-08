package org.act.tgraph.demo.utils;


import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.sun.management.OperatingSystemMXBean;
import org.act.tgraph.demo.Config;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class TGraphServer {
    private final String dbPath;
    private final ReqExecutor reqExecutor;

    private final Producer logger;
    private GraphDatabaseService db;
    private volatile boolean shouldRun = true;
    private ServerSocket server;
    private final List<Thread> threads = Collections.synchronizedList(new LinkedList<>());
    private final String serverCodeVersion;

    public TGraphServer(Config config, String dbPath, ReqExecutor reqExecutor) {
        this.logger = config.onlineLogger;
        this.dbPath = dbPath;
        this.reqExecutor = reqExecutor;
        serverCodeVersion = config.gitStatus;
        System.out.println("server code version: "+ serverCodeVersion);
    }

    public void start() throws IOException, ProducerException, InterruptedException {
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
        reqExecutor.setDB(db);

        server = new ServerSocket(8438);
        System.out.println("waiting for client to connect.");

        try {
            while (shouldRun) {
                Socket client;
                try {
                    client = server.accept();
                } catch (SocketException ignore) { // closed from another thread.
                    break;
                }
                Thread t = new ServerThread(client, monitor);
                threads.add(t);
                System.out.println("GET one more client, currently " + threads.size() + " client");
                t.setDaemon(true);
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }
        }catch (InterruptedException ignore){
            // just exit
        }
        db.shutdown();
        logger.close();
        System.out.println("main thread exit.");
    }

    public static abstract class ReqExecutor {
        protected GraphDatabaseService db;
        private void setDB(GraphDatabaseService db){
            this.db = db;
        }
        protected abstract String execute(String line) throws RuntimeException;
    }

    public static class TransactionFailedException extends RuntimeException{}

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
                        System.out.println("closed by server.");
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
                    String exeResult = "";
                    boolean success = true;
                    try {
                        exeResult = reqExecutor.execute(line);
                    }catch (TransactionFailedException e){
                        success = false;
                    }
                    time.mark("Transaction", "Send");
                    String result = generateResult(
                            exeResult,
                            success,
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

        private String generateResult(String results, boolean success, long reqGotTime, long previousSendTime, long txTime, ServerStatus s) {
            JsonObject obj = new JsonObject();
            obj.add("success", success);
            obj.add("resultSize", results.length());
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
