package org.act.tgraph.demo.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *  create by sjh at 2019-09-23
 *
 *  Test TGraph Server performance.
 */
public abstract class TGraphSocketClient {
    private BlockingQueue<Connection> connectionPool = new LinkedBlockingQueue<>();
    private ThreadPoolExecutor service;

    /**
     * Arguments:
     * @param serverHost hostname of TGraph (TCypher) server.
     * @param parallelCnt number of threads to send queries.
     * @param queueLength queue to cache data/request read from disk.
     */
    public TGraphSocketClient(String serverHost, int parallelCnt, int queueLength) throws IOException {
        this.service = new ThreadPoolExecutor(parallelCnt, parallelCnt, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueLength), new ThreadPoolExecutor.CallerRunsPolicy());
        for(int i = 0; i< parallelCnt; i++) this.connectionPool.offer(new Connection(serverHost, 8438));
        this.service.prestartAllCoreThreads();
    }

    public Future<String> addQuery(String query) {
        return service.submit(new Req(query));
    }

    public void awaitTermination() throws InterruptedException, IOException {
        service.shutdown();
        while(!service.isTerminated()) {
            service.awaitTermination(10, TimeUnit.SECONDS);
            System.out.println( service.getCompletedTaskCount()+" query completed.");
        }
        while(true){
            Connection conn = connectionPool.poll();
            if(conn!=null) conn.close();
            else break;
        }
        System.out.println("Client exit. send "+ service.getCompletedTaskCount() +" lines.");
    }

    public class Req implements Callable<String> {
        private final TimeMonitor timeMonitor = new TimeMonitor();
        private final String query;

        Req(String query){
            this.query = query;
            timeMonitor.begin("Wait in queue");
        }

        @Override
        public String call() throws Exception {
            try {
                Connection conn = connectionPool.take();
                timeMonitor.mark("Wait in queue", "Send query");
                conn.out.println(query);
                timeMonitor.mark("Send query", "Wait result");
                String response = conn.in.readLine();
                timeMonitor.end("Wait result");
                if (response == null) throw new RuntimeException("[Got null. Server close connection]");
                onResponse(query, response, timeMonitor, Thread.currentThread(), conn);
                connectionPool.put(conn);
                return response;
            }catch (Exception e){
                e.printStackTrace();
                throw e;
            }
        }
    }

    protected abstract void onResponse(String query, String response, TimeMonitor timeMonitor, Thread thread, Connection conn) throws Exception;

    public static class Connection{
        private Socket client;
        BufferedReader in;
        PrintWriter out;

        Connection(String host, int port) throws IOException {
            this.client = new Socket(host, port);
//            client.setSoTimeout(8000);
            this.client.setTcpNoDelay(true);
            this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            this.out = new PrintWriter(client.getOutputStream(), true);
        }

        public void close() throws IOException {
            in.close();
            out.close();
            client.close();
        }
    }
}
