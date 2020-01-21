package org.act.tgraph.demo.utils;


import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.aliyun.openservices.log.common.LogItem;
import com.sun.management.OperatingSystemMXBean;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TCypherServerOneConnection {
    private final String dbPath;
    private final int maxConcurrentCnt;
    private final int waitingQueueSize;

    private final RuntimeStatus jvm = new RuntimeStatus();
    private final Producer logger;
    private GraphDatabaseService db;


    public TCypherServerOneConnection(int maxConcurrentCnt, int waitingQueueSize, Producer logger, String dbPath) {
        this.maxConcurrentCnt = maxConcurrentCnt;
        this.waitingQueueSize = waitingQueueSize;
        this.logger = logger;
        this.dbPath = dbPath;
    }


    public void start() throws IOException {
        db = new GraphDatabaseFactory().newEmbeddedDatabase( new File(dbPath));
        jvm.runtime.addShutdownHook(new Thread(() -> {
            db.shutdown();
            try {
                logger.close();
            } catch (InterruptedException | ProducerException e) {
                e.printStackTrace();
            }
        }));

        BlockingQueue<Runnable> threadQueue = new ArrayBlockingQueue<>(waitingQueueSize);
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(maxConcurrentCnt, maxConcurrentCnt,0L, TimeUnit.MILLISECONDS, threadQueue);

        ServerSocket server = new ServerSocket(8438);
        System.out.println("waiting for client to connect.");
        Socket client = server.accept();
        BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintStream toClient = new PrintStream(client.getOutputStream(), true);

        long socket_wait_time = 0;
        while(true){
            toClient.println(threadQueue.size());
            long t0 = System.currentTimeMillis();
            String line = fromClient.readLine();
            if("EXIT".equals(line)) break;
            long t1 = System.currentTimeMillis();
            socket_wait_time += t1 - t0;
            try {
                Req req = new Req();
                req.queries = line.split(";");
                threadPool.execute(req);
                req.addInQueueTime = System.currentTimeMillis();
                toClient.println("GOT");
            }catch(RejectedExecutionException ignored){
                toClient.println("SERVER BUSY");
            }
        }
        threadPool.shutdown();
        while(true){
            try {
                threadPool.awaitTermination(100, TimeUnit.MILLISECONDS);
                System.out.println("DONE. process "+threadPool.getTaskCount()+" task. socket wait "+ socket_wait_time +" milliseconds");
                return;
            }catch (InterruptedException ignored){}
        }
    }


    private class Req implements Runnable{

        long createTime = System.currentTimeMillis();
        long addInQueueTime;
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
            LogItem log = createLogLine();
            log.PushBack( "tx_start_t_milli", String.valueOf(txBeginTime));
            log.PushBack("tx_end_t_milli", String.valueOf(txEndTime));
            log.PushBack("tx_success", String.valueOf(success));
            log.PushBack("rq_enqueue_t_milli", String.valueOf(addInQueueTime));
            log.PushBack("rq_create_t_milli", String.valueOf(createTime));
            log.PushBack("rq_result_size", String.valueOf(resultSize));
            try {
                logger.send("tgraph-demo-test", "tgraph-log", "kernel-write-2019.9.10-v2", "sjh-ubuntu1804", log);
            } catch (InterruptedException | ProducerException e) {
                e.printStackTrace();
            }
        }

        private LogItem createLogLine() {
            LogItem log = new LogItem();
            log.PushBack("vm_memory", String.valueOf(jvm.currentMemUsage()));
            log.PushBack("thread", Thread.currentThread().getName());
            log.PushBack("tx_data_cnt", String.valueOf(queries.length));
            log.PushBack("queue_length", String.valueOf(waitingQueueSize));
            return log;
        }
    }

    private static class RuntimeStatus{
        long memoryUsage;
        double cpuTime, newCpuTime;
        static final OperatingSystemMXBean bean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        static final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();
        Runtime runtime = Runtime.getRuntime();
        long currentMemUsage(){
            return runtime.totalMemory() - runtime.freeMemory();
        }
        long getCPUTime(){
            return bean.getProcessCpuTime();
        }
    }

}
