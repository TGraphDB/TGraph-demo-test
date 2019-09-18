package org.act.tgraph.demo.utils;


import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.aliyun.openservices.log.common.LogItem;
import com.sun.management.OperatingSystemMXBean;
import org.act.tgraph.demo.Config;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class TCypherServer {
    private final String dbPath;
    private final String SEPARATOR;

    private final Producer logger;
    private GraphDatabaseService db;
    private volatile boolean shouldRun = true;
    private ServerSocket server;
    private String testTopic;
    private final List<Thread> threads = new LinkedList<>();
    private final String serverCodeVersion;

    public static void main(String[] args){
        System.out.println(args[0]);
        TCypherServer server = new TCypherServer("AMITABHA", Config.Default.onlineLogger, args[0]);
        try {
            server.start();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public TCypherServer(String separator, Producer logger, String dbPath) {
        this.SEPARATOR = separator;
        this.logger = logger;
        this.dbPath = dbPath;
        serverCodeVersion = getGitDesp();
    }

    private String getGitDesp() {
        try (InputStream input = this.getClass().getResourceAsStream("git.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty("git.commit.id.describe-short");
        } catch (IOException ex) {
            ex.printStackTrace();
            return "Git-None";
        }
    }


    public void start() throws IOException, InterruptedException {
        db = new GraphDatabaseFactory().newEmbeddedDatabase( new File(dbPath));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            db.shutdown();
            try {
                logger.close();
            } catch (InterruptedException | ProducerException e) {
                e.printStackTrace();
            }
        }));
        new MonitorThread().start();

        server = new ServerSocket(8438);
        System.out.println("waiting for client to connect.");

        while(shouldRun) {
            Socket client;
            try {
                client = server.accept();
            }catch (SocketException ignore){ // closed from another thread.
                break;
            }
            Thread t = new ServerThread(client);
            threads.add(t);
            t.start();
        }
        for(Thread t : threads){
            t.join();
        }
    }


    private class Req implements Runnable{
        long createTime = System.currentTimeMillis();
        long socketWaitTime;
        long txBeginTime;
        long txEndTime;
        String[] queries;
        String[] results = new String[0];
        int resultSize;
        boolean success=true;

        @Override
        public void run() {
            results = new String[queries.length];
            txBeginTime = System.currentTimeMillis();
            try {
                try (Transaction tx = db.beginTx()) {
                    for (int i = 0; i < queries.length; i++) {
                        String query = queries[i];
                        Result result = db.execute(query);
                        results[i] = result.resultAsString();
                        resultSize += results[i].length();
                    }
                    tx.success();
                }
            }catch (Exception msg){
                success = false;
            }
            txEndTime = System.currentTimeMillis();
        }
    }

    private class MonitorThread extends Thread{
        public void run(){
            final OperatingSystemMXBean bean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            Runtime runtime = Runtime.getRuntime();
            try {
                while(shouldRun){
                    Thread.sleep(1_000);
                    long curMem = runtime.totalMemory() - runtime.freeMemory();
                    double processCpuLoad = bean.getProcessCpuLoad();
                    double systemCpuLoad = bean.getSystemCpuLoad();
                    int activeConn = threads.size();
                    LogItem log = new LogItem();
                    log.PushBack("timestamp", String.valueOf(System.currentTimeMillis()));
                    log.PushBack("vm_memory", String.valueOf(curMem));
                    log.PushBack("thread_cnt", String.valueOf(activeConn));
                    if(!(processCpuLoad<0)) log.PushBack("process_load", String.valueOf(processCpuLoad));
                    if(!(systemCpuLoad<0)) log.PushBack("system_load", String.valueOf(systemCpuLoad));
                    logger.send("tgraph-demo-test", "tgraph-log", testTopic, "sjh-ubuntu1804", log);
                }
            } catch (InterruptedException | ProducerException e) {
                e.printStackTrace();
            }
        }
    }

    private class ServerThread extends Thread{
        Socket client;
        BufferedReader fromClient;
        PrintStream toClient;

        ServerThread(Socket client) throws IOException {
            this.client = client;
            this.fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            this.toClient = new PrintStream(client.getOutputStream(), true);
        }

        public void run(){
            long tid = Thread.currentThread().getId();
            Thread.currentThread().setName("TCypher con "+tid);
            System.out.println(Thread.currentThread().getName()+" started.");
            try {
                while(true){
                    long t0 = System.currentTimeMillis();
                    String line;
                    try {
                        line = fromClient.readLine();
                    }catch (SocketException ignore){// client close conn.
                        System.out.println("client close connection.");
                        break;
                    }
                    if("EXIT".equals(line)){ //client ask server exit;
                        client.close();
                        server.close();
                        shouldRun = false;
                        System.out.println("client ask server exit.");
                        break;
                    }
                    if("GC".equals(line)){
                        Runtime.getRuntime().gc();
                        System.out.println("client ask server gc.");
                        continue;
                    }
                    if(line.startsWith("TOPIC:")){
                        testTopic = line.substring(6)+"-"+serverCodeVersion;
                        System.out.println("topic changed to "+testTopic);
                        continue;
                    }
                    long t1 = System.currentTimeMillis();
                    Req req = new Req();
                    req.socketWaitTime = t1 - t0;
                    req.queries = line.split(";");
                    req.run();
                    toClient.println(
                            req.success + SEPARATOR + req.resultSize + SEPARATOR +
                                    req.createTime + SEPARATOR +
                                    req.socketWaitTime + SEPARATOR +
                                    req.txBeginTime + SEPARATOR +
                                    req.txEndTime + SEPARATOR +
                                    String.join(SEPARATOR, req.results)
                    );
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            threads.remove(this);
            System.out.println(Thread.currentThread().getName()+" exit.");
        }
    }

}
