package org.act.tgraph.demo.client;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.act.tgraph.demo.utils.Helper;
import org.act.tgraph.demo.utils.TimeMonitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.*;

/**
 *  create by sjh at 2019-09-23
 *
 *  Test TGraph Server performance.
 */
public abstract class TGraphSocketClient {
    private final ThreadPoolExecutor exe;
    private BlockingQueue<Connection> connectionPool = new LinkedBlockingQueue<>();
    private ListeningExecutorService service;

    /**
     * Arguments:
     * @param serverHost hostname of TGraph (TCypher) server.
     * @param parallelCnt number of threads to send queries.
     * @param queueLength queue to cache data/request read from disk.
     */
    public TGraphSocketClient(String serverHost, int parallelCnt, int queueLength) throws IOException, ExecutionException, InterruptedException {
        this.exe = new ThreadPoolExecutor(parallelCnt, parallelCnt, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueLength), (r, executor) -> {
            if (!executor.isShutdown()) try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        exe.prestartAllCoreThreads();
        this.service = MoreExecutors.listeningDecorator(exe);
        for(int i = 0; i< parallelCnt; i++) this.connectionPool.offer(new Connection(serverHost, 8438));
        testServerClientCompatibility();
    }

    public ListenableFuture<JsonObject> addQuery(String query) {
        return service.submit(new Req(query));
    }

    private void testServerClientCompatibility() throws ExecutionException, InterruptedException {
        Future<JsonObject> response = addQuery("VERSION");
        String result = response.get().get("result").asString();
        String clientVersion = Helper.currentCodeVersion();
        if(!clientVersion.equals(result)) throw new UnsupportedOperationException(String.format("server(%s) client(%s) version not match!", result, clientVersion));
    }

    public void awaitTermination() throws InterruptedException, IOException {
        service.shutdown();
        while(!service.isTerminated()) {
            service.awaitTermination(10, TimeUnit.SECONDS);
            long completeCnt = exe.getCompletedTaskCount();
            int remains = exe.getQueue().size();
            System.out.println( completeCnt+"/"+ (completeCnt+remains)+" query completed.");
        }
        while(true){
            Connection conn = connectionPool.poll();
            if(conn!=null) conn.close();
            else break;
        }
        System.out.println("Client exit. send "+ exe.getCompletedTaskCount() +" lines.");
    }

    public class Req implements Callable<JsonObject> {
        private final TimeMonitor timeMonitor = new TimeMonitor();
        private final String query;

        Req(String query){
            this.query = query;
            timeMonitor.begin("Wait in queue");
        }

        @Override
        public JsonObject call() throws Exception {
            try {
                Connection conn = connectionPool.take();
                timeMonitor.mark("Wait in queue", "Send query");
                conn.out.println(query);
                timeMonitor.mark("Send query", "Wait result");
                String response = conn.in.readLine();
                timeMonitor.end("Wait result");
                if (response == null) throw new RuntimeException("[Got null. Server close connection]");
                connectionPool.put(conn);
                return onResponse(query, response, timeMonitor, Thread.currentThread());
            }catch (Exception e){
                e.printStackTrace();
                throw e;
            }
        }
    }

    protected abstract JsonObject onResponse(String query, String response, TimeMonitor timeMonitor, Thread thread) throws Exception;

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
