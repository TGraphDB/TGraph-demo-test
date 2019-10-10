package org.act.tgraph.demo.utils;

import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.log.common.LogItem;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.act.tgraph.demo.Config;
import org.act.tgraph.demo.vo.RuntimeEnv;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 *  create by sjh at 2019-09-23
 *
 *  Test TGraph Server TCypher performance.
 */
public class TCypherClient  {
    private final boolean enableResultLog;
    private final int queueLength;
    private String testName;
    private String logSource;
    private final String serverHost;
    private final int threadCnt;

    private long querySendCnt = 0;
    private BlockingQueue<Connection> connectionPool = new LinkedBlockingQueue<>();
    private ThreadPoolExecutor service;
    private Producer logger;

    /**
     *
     * Arguments:
     * 1. [testName] name of the test. time and git info is auto appended.
     * 2. [serverHost] hostname of TGraph (TCypher) server.
     * 3. [threadCount] number of threads to send queries.
     */
    public TCypherClient(String testName, String serverHost, int threadCnt, int queueLength, boolean enableResultLog){
        SimpleDateFormat ft = new SimpleDateFormat ("yyyyMMdd_HHmm");
        this.testName = testName + "-" + ft.format(new Date());
        this.logSource = RuntimeEnv.getCurrentEnv().name();
        this.serverHost = serverHost;
        this.threadCnt = threadCnt;
        this.queueLength = queueLength;
        this.enableResultLog = enableResultLog;
    }

    public Map<String, Long> start() throws InterruptedException, ExecutionException, IOException {
        Config config = RuntimeEnv.getCurrentEnv().getConf();
        this.logger = config.getLogger();
        this.service = new ThreadPoolExecutor(threadCnt, threadCnt, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueLength), new ThreadPoolExecutor.CallerRunsPolicy());
        for(int i=0; i<threadCnt; i++) this.connectionPool.offer(new Connection(serverHost, 8438));

        String responseLine = this.addQuery("TOPIC:"+testName).get();
        if(responseLine.startsWith("Server code version:")) {
            String serverCodeVersion = responseLine.substring(20);
            String gitVersion = config.codeGitVersion();
            if (Objects.equals(serverCodeVersion.split(".")[1], gitVersion)){
                this.logSource += "-" + serverCodeVersion;
            }else{
                this.logSource += "."+gitVersion+"-" + serverCodeVersion;
            }
            System.out.println("logSource: "+ logSource);
        }else throw new RuntimeException("unexpected server response.");

        Future<String> r = this.addQuery("ID MAP");
        System.out.println("wait id map from server...");
        long t = System.currentTimeMillis();
        String relIDMapJsonStr = r.get();
        System.out.println("done. wait "+(System.currentTimeMillis()-t)/1000+" seconds.");
//        System.out.println(relIDMapJsonStr);
        Map<String, Long> idMap = new HashMap<>(140000);
        JsonObject obj = Json.parse(relIDMapJsonStr).asObject();
        for (JsonObject.Member m : obj) {
            idMap.put(m.getName(), m.getValue().asLong());
        }
        service.prestartAllCoreThreads();
        return idMap;
    }

    public Future<String> addQuery(String query) {
        Future<String> r = service.submit(new Req(query));
        querySendCnt++;
        if(querySendCnt %400==0) System.out.println(querySendCnt +" query added.");
        return r;
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
        System.out.println("Client exit. send "+ querySendCnt +" lines.");
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
                if (response.startsWith("Server code version:")) {
                    return response;
                } else if ("ID MAP".equals(query)) {
                    return response;
                }
                JsonObject result = Json.parse(response).asObject();
                String resultContent = result.get("results").asString();
                LogItem log = new LogItem();
                log.PushBack("type", "time");
                log.PushBack("c_thread", conn.toString());
                log.PushBack("c_queue_t", String.valueOf(timeMonitor.duration("Wait in queue")));
                log.PushBack("c_send_t", String.valueOf(timeMonitor.duration("Send query")));
                log.PushBack("c_send_tE", String.valueOf(timeMonitor.endT("Send query")));
                log.PushBack("c_wait_t", String.valueOf(timeMonitor.duration("Wait result")));

                log.PushBack("s_receive_tE", String.valueOf(result.get("t_ReqGot").asLong()));
                log.PushBack("s_tx_t", String.valueOf(result.get("t_Tx").asLong()));
                log.PushBack("s_psend_t", String.valueOf(result.get("t_PreSend").asLong()));
                log.PushBack("s_tx_success", String.valueOf(result.get("success").asBoolean()));
                log.PushBack("s_result_size", String.valueOf(resultContent.length()));

                log.PushBack("v_update_t", String.valueOf(result.get("s_updateTime").asLong()));
                log.PushBack("v_memory", String.valueOf(result.get("s_memory").asLong()));
                log.PushBack("v_connCnt", String.valueOf(result.get("s_connCnt").asLong()));
                log.PushBack("v_pCPU", String.valueOf(result.get("s_pCPU").asDouble()));
                log.PushBack("v_CPU", String.valueOf(result.get("s_CPU").asDouble()));
                log.PushBack("v_disk_qLen", String.valueOf(result.get("s_disk_qLen").asLong()));
                log.PushBack("v_disk_read", String.valueOf(result.get("s_disk_read").asLong()));
                log.PushBack("v_disk_write", String.valueOf(result.get("s_disk_write").asLong()));

                logger.send("tgraph-demo-test", "tgraph-log", testName, logSource, log);
                if (enableResultLog) {
                    LogItem resultLog = new LogItem();
                    resultLog.PushBack("type", "result");
                    resultLog.PushBack("c_send_tE", String.valueOf(timeMonitor.endT("Send query")));
                    resultLog.PushBack("c_thread", conn.toString());
                    resultLog.PushBack("s_result_content", resultContent);
                    logger.send("tgraph-demo-test", "tgraph-log", testName, logSource, resultLog);
                }
                connectionPool.offer(conn);
                return response;
            }catch (Exception e){
                e.printStackTrace();
                throw e;
            }
        }
    }



    private static class Connection{
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
