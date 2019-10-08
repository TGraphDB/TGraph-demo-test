package org.act.tgraph.demo.utils;

import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.aliyun.openservices.log.common.LogItem;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import org.act.tgraph.demo.Config;
import org.act.tgraph.demo.vo.RuntimeEnv;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *  create by sjh at 2019-09-23
 *
 *  Test TGraph Server TCypher performance.
 */
public class TCypherClient {
    private final int queueLength;
    private final boolean enableResultLog;
    private String testName;
    private String logSource;
    private final String serverHost;
    private final int threadCnt;

    private long querySendCnt = 0;
    private volatile boolean complete = false;
    private BlockingQueue<String> queue;
    private List<Thread> threads;

    private String relIDMapJsonStr;
    /**
     *
     * has 7 arguments:
     * 1. [testName] name of the test. time and git info is auto appended.
     * 2. [serverHost] hostname of TGraph (TCypher) server.
     * 3. [threadCount] number of threads to send queries.
     */
    public TCypherClient(String testName, String logSource, String serverHost, int threadCnt, int queueLength, boolean enableResultLog){
        SimpleDateFormat ft = new SimpleDateFormat ("yyyyMMdd_HHmm");
        this.testName = testName + "-" + ft.format(new Date());
        this.logSource = logSource;
        this.serverHost = serverHost;
        this.threadCnt = threadCnt;
        this.queueLength = queueLength;
        this.enableResultLog = enableResultLog;
    }

    public synchronized Map<String, Long> start() throws IOException, InterruptedException {
        queue = new ArrayBlockingQueue<>(queueLength);
        threads = new LinkedList<>();
        for(int i=0; i<threadCnt; i++) {
            Thread t = new SendingThread(serverHost, 8438, queue);
            threads.add(t);
            t.setDaemon(true);
            t.start();
        }
        queue.put("TOPIC:"+testName);
        String q = "ID MAP";
        queue.put(q);
        System.out.println("wait id map from server...");
        long t = System.currentTimeMillis();
        q.wait();
        System.out.println("wake up. wait "+(System.currentTimeMillis()-t)/1000+" seconds.");
        if(relIDMapJsonStr!=null){
            Map<String, Long> idMap = new HashMap<>(140000);
            JsonObject obj = Json.parse(relIDMapJsonStr).asObject();
            for (JsonObject.Member m : obj) {
                idMap.put(m.getName(), m.getValue().asLong());
            }
            return idMap;
        }else{
            throw new RuntimeException("not get id map but wake up.");
        }
    }

    public void addQuery(String query) throws InterruptedException {
        queue.put(query);
        querySendCnt++;
        if(querySendCnt %400==0) System.out.println(querySendCnt +" query added.");
    }

    public void awaitSendDone() throws InterruptedException, ProducerException {
        while(queue.size()>0) {
            if(threads.size()==0) {
                break;
            }
            Thread.sleep(10000);
            System.out.println("queue size: "+queue.size());
        }
        complete = true;
        for(Thread t : threads) t.join();
        System.out.println("Client exit. send "+ querySendCnt +" lines.");
    }

    private class SendingThread extends Thread{
        Socket client;
        BufferedReader in;
        PrintWriter output;
        BlockingQueue<String> queue;
        SendingThread(String host, int port, BlockingQueue<String> queue) throws IOException {
            this.queue = queue;
            client = new Socket(host, port);
//            client.setSoTimeout(8000);
            client.setTcpNoDelay(true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            output = new PrintWriter(client.getOutputStream(), true);
        }

        public synchronized void run(){
            Thread t = Thread.currentThread();
            t.setName("TCypher-client-"+t.getId());
            Producer logger = RuntimeEnv.getCurrentEnv().getConf().getLogger();
            String serverCodeVersion = "";
            TimeMonitor timeMonitor = new TimeMonitor();
            try {
                timeMonitor.begin("Thread run");
                timeMonitor.begin("Log");
                while(!complete){
                    timeMonitor.mark("Log", "Read query");
                    long previousLogT = timeMonitor.duration("Log");
                    String query;
                    try {
                        query = queue.poll(2, TimeUnit.SECONDS);
                        if(query==null) continue;
                    } catch (InterruptedException e) {
                        continue;
                    }
                    timeMonitor.mark("Read query", "Send query");
                    output.println(query);
                    timeMonitor.mark("Send query", "Wait result");
                    JsonObject result;
                    try {
                        String response = in.readLine();
                        timeMonitor.mark("Wait result", "Log");
                        if(response==null) break;
                        if(response.startsWith("Server code version:")){
                            serverCodeVersion = response.substring(20);
                            logSource += "-" + serverCodeVersion;
                            continue;
                        }else if("ID MAP".equals(query)){
                            relIDMapJsonStr = response;
                            query.notify();
                            timeMonitor.begin("Log");
                            continue;
                        }
                        result = Json.parse(response).asObject();
                    } catch (IOException e) {
                        System.out.println("Server close connection.");
                        e.printStackTrace();
                        break;
                    }
                    JsonArray resultArr = result.get("results").asArray();
                    LogItem log = new LogItem();
                    log.PushBack("type", "time");
                    log.PushBack("c_thread", t.getName());
                    log.PushBack("c_read_t", String.valueOf(timeMonitor.duration("Read query")));
                    log.PushBack("c_send_t", String.valueOf(timeMonitor.duration("Send query")));
                    log.PushBack("c_send_tE", String.valueOf(timeMonitor.endT("Send query")));
                    log.PushBack("c_wait_t", String.valueOf(timeMonitor.duration("Wait result")));
                    log.PushBack("c_plog_t", String.valueOf(previousLogT));

                    log.PushBack("s_receive_tE", String.valueOf(result.get("t_ReqGot").asLong()));
                    log.PushBack("s_tx_t", String.valueOf(result.get("t_Tx").asLong()));
                    log.PushBack("s_psend_t", String.valueOf(result.get("t_PreSend").asLong()));
                    log.PushBack("s_tx_success", String.valueOf(result.get("success").asBoolean()));
                    log.PushBack("s_tx_line_cnt", String.valueOf(resultArr.size()));

                    log.PushBack("v_update_t", String.valueOf(result.get("s_updateTime").asLong()));
                    log.PushBack("v_memory", String.valueOf(result.get("s_memory").asLong()));
                    log.PushBack("v_connCnt", String.valueOf(result.get("s_connCnt").asLong()));
                    log.PushBack("v_pCPU", String.valueOf(result.get("s_pCPU").asDouble()));
                    log.PushBack("v_CPU", String.valueOf(result.get("s_CPU").asDouble()));
                    log.PushBack("v_disk_qLen", String.valueOf(result.get("s_disk_qLen").asLong()));
                    log.PushBack("v_disk_read", String.valueOf(result.get("s_disk_read").asLong()));
                    log.PushBack("v_disk_write", String.valueOf(result.get("s_disk_write").asLong()));

                    try {
                        logger.send("tgraph-demo-test", "tgraph-log", testName, logSource, log);
                        if(enableResultLog){
                            for(int i=0; i<resultArr.size(); i++) {
                                LogItem resultLog = new LogItem();
                                resultLog.PushBack("type", "result");
                                resultLog.PushBack("c_send_tE", String.valueOf(timeMonitor.endT("Send query")));
                                resultLog.PushBack("c_thread", t.getName());
                                resultLog.PushBack("s_result_num", String.valueOf(i));
                                resultLog.PushBack("s_result_content", resultArr.get(i).asString());
                                logger.send("tgraph-demo-test", "tgraph-log", testName, "sjh-ubuntu1804", resultLog);
                            }
                        }
                    } catch (InterruptedException | ProducerException e) {
                        e.printStackTrace();
                    }
                }
                in.close();
                output.close();
                client.close();
                System.out.println("client connection close.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            timeMonitor.end("Thread run");
            System.out.println("client thread exit. runs "+ timeMonitor.duration("Thread run")/1000+" seconds.");
        }
    }
}
