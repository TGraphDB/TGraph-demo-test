package org.act.temporal.test.tcypher;

import com.aliyun.openservices.aliyun.log.producer.LogProducer;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.aliyun.log.producer.ProducerConfig;
import com.aliyun.openservices.aliyun.log.producer.ProjectConfig;
import com.aliyun.openservices.aliyun.log.producer.errors.ProducerException;
import com.aliyun.openservices.log.common.LogItem;
import com.sun.management.OperatingSystemMXBean;
import org.act.tgraph.demo.vo.Cross;
import org.act.tgraph.demo.vo.RelType;
import org.act.tgraph.demo.vo.RoadChain;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class TestServer {
    int maxConcurrentCnt = 3;
    static int waitingQueueSize = 200;
    static int transDataCnt = 4;
    int size = 2000000;

    private static RuntimeStatus jvm = new RuntimeStatus();

    @Test
    public void server() throws IOException, ParseException, InterruptedException {
        Req.logger = getLogger();
        Req.db = new GraphDatabaseFactory().newEmbeddedDatabase( new File("/media/song/test/db-network-only"));
        jvm.runtime.addShutdownHook(new Thread(() -> {
            Req.db.shutdown();
            try {
                Req.logger.close();
            } catch (InterruptedException | ProducerException e) {
                e.printStackTrace();
            }
        }));
        Req.roadMap = buildRoadIDMap(Req.db);


        BlockingQueue<Runnable> threadQueue = new ArrayBlockingQueue<>(waitingQueueSize);
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(maxConcurrentCnt, maxConcurrentCnt,0L, TimeUnit.MILLISECONDS, threadQueue);

        ServerSocket server = new ServerSocket(8438);
        System.out.println("waiting for client to connect.");
        Socket client = server.accept();
        BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintStream writer = new PrintStream(client.getOutputStream(), true);

        long socket_wait_time = 0;
        for(int dataCount = 0; dataCount<size;  ){
            while(threadQueue.size() < waitingQueueSize && dataCount<size){//System.out.println(ThreadQueue.size());
                writer.println(threadQueue.size());
                String[] lines = new String[transDataCnt];
                for (int i = 0; i< transDataCnt && dataCount<size; i++){//System.out.println(s);
                    long t0 = System.currentTimeMillis();
                    lines[i] = input.readLine();
                    long t1 = System.currentTimeMillis();
                    socket_wait_time += t1 - t0;
                    //System.out.println(lines[i]);
                    dataCount++;
                }
                addToQueue(threadPool, string2req(lines));
                writer.println(threadQueue.size());
            }
            if(dataCount%1000==0) System.out.println("process "+dataCount+" lines");
        }
        while(threadQueue.size()>0){
            System.out.println("ALL in queue. "+threadQueue.size()+" task remains.");
            Thread.sleep(4000);
        }
        System.out.println("DONE. process "+threadPool.getTaskCount()+" task. socket wait "+ socket_wait_time +" milliseconds");
        threadPool.shutdown();
    }

    @Test
    public void importNetwork2db() throws IOException {

        List<RoadChain> roadChainList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("/home/song/tmp/Topo.csv"))) {
            String line;
            for (int lineCount = 0; (line = br.readLine()) != null; lineCount++) {
                if (lineCount == 0) continue;//ignore headers
                try {
                    roadChainList.add(new RoadChain(line, lineCount));
                }catch (RuntimeException e){
                    System.out.println(e.getMessage()+" at line:"+lineCount);
                }
            }
        }
        for(RoadChain roadChain: roadChainList){
            roadChain.updateNeighbors();
        }

        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File("/tmp/test-db"));
        try(Transaction tx = db.beginTx()) {
            for (RoadChain roadChain : roadChainList) {
                int inCount = roadChain.getInNum();
                int outCount = roadChain.getOutNum();
                if (inCount > 0 || outCount > 0) {
                    Cross inCross = Cross.getStartCross(roadChain);
                    Cross outCross = Cross.getEndCross(roadChain);
                    Node inNode, outNode;
                    if (inCross.getNode(db) == null) {
                        inNode = db.createNode();
                        inCross.setNode(inNode);
                        inNode.setProperty("cross_id", inCross.id);
                    } else {
                        inNode = inCross.getNode(db);
                    }
                    if (outCross.getNode(db) == null) {
                        outNode = db.createNode();
                        outCross.setNode(outNode);
                        outNode.setProperty("cross_id", outCross.id);
                    } else {
                        outNode = outCross.getNode(db);
                    }

                    Relationship r = inNode.createRelationshipTo(outNode, RelType.ROAD_TO);
                    r.setProperty("grid_id", roadChain.getGridId());
                    r.setProperty("chain_id", roadChain.getChainId());
//                    r.setProperty("uid", roadChain.getUid());
//                    r.setProperty("type", roadChain.getType());
//                    r.setProperty("length", roadChain.getLength());
//                    r.setProperty("angle", roadChain.getAngle());
//                    r.setProperty("in_count", roadChain.getInNum());
//                    r.setProperty("out_count", roadChain.getOutNum());
//                    r.setProperty("in_roads", roadChain.briefInChain());
//                    r.setProperty("out_roads", roadChain.briefOutChain());
//                    r.setProperty("data_count", 0);
//                    r.setProperty("min_time", Integer.MAX_VALUE);
//                    r.setProperty("max_time", 0);
                }
            }
            tx.success();
        }
        db.shutdown();
    }

    private Map<String, Relationship> buildRoadIDMap(GraphDatabaseService db) throws IOException {
        Map<String, Relationship> map = new HashMap<>(130000);
        try(Transaction tx = db.beginTx()){
            for(Relationship r: GlobalGraphOperations.at(db).getAllRelationships()){
                String gridId = (String) r.getProperty("grid_id");
                String chainId = (String) r.getProperty("chain_id");
                String key = gridId+","+chainId;
                map.put(key, r);
            }
            tx.success();
        }
        return map;
    }

    private Producer getLogger(){
        ProducerConfig pConf = new ProducerConfig();
        pConf.setIoThreadCount( 1 ); // one thread to upload
        Producer producer = new LogProducer( pConf );
        producer.putProjectConfig(new ProjectConfig("tgraph-demo-test", "cn-beijing.log.aliyuncs.com", "LTAIeA55PGyOpgZs", "H0XzCznABsioSI4TQpwXblH269eBm6"));
        return producer;
    }

    private void addToQueue(ExecutorService threadPool, Req req) {
        while(true){
            try {
                threadPool.execute(req);
                req.addInQueueTime = System.currentTimeMillis();
                return;
            }catch(RejectedExecutionException ignored){
                // try again;
            }
        }
    }

    private Req string2req(String[] lines) throws ParseException {
        int i = 0;
        DataLine[] data = new DataLine[lines.length];
        for (String line : lines) {
            String[] arr = line.split(":");
            DataLine dLine = new DataLine();
            dLine.time = parseTime(arr[0]);
            dLine.road = Req.roadMap.get(arr[1]);
            if(dLine.road==null) throw new RuntimeException("road(" + arr[1] + ") not found");
            String[] d = arr[2].split(",");
            dLine.travelTime = Integer.parseInt(d[0]);
            dLine.fullStatus = Integer.parseInt(d[1]);
            dLine.vehicleCount = Integer.parseInt(d[2]);
            dLine.segmentCount = Integer.parseInt(d[3]);
            data[i] = dLine;
            i++;
        }
        Req req = new Req();
        req.data = data;
        return req;
    }

    private SimpleDateFormat timeParser = new java.text.SimpleDateFormat("yyyyMMddHHmm");
    private int parseTime(String s) throws ParseException {
        return Math.toIntExact(timeParser.parse("20"+s).getTime()/1000);
    }

    private static class DataLine{
        Relationship road;
        int time;
        int travelTime;
        int fullStatus;
        int vehicleCount;
        int segmentCount;
    }

    private static class Req implements Runnable{
        static Map<String, Relationship> roadMap;
        static Producer logger;
        static GraphDatabaseService db;

        long createTime = System.currentTimeMillis();
        long addInQueueTime;
        long txBeginTime;
        long txEndTime;
        DataLine[] data;

        @Override
        public void run() {
            txBeginTime = System.currentTimeMillis();
            try(Transaction tx = db.beginTx()){
                for(DataLine item: data){
                    Relationship r = item.road;
                    r.setTemporalProperty("travel_time", item.time, item.travelTime);
                    r.setTemporalProperty("full_status", item.time, item.fullStatus);
                    r.setTemporalProperty("vehicle_count", item.time, item.vehicleCount);
                    r.setTemporalProperty("segment_count", item.time, item.segmentCount);
                }
                tx.success();
            }
            txEndTime = System.currentTimeMillis();
            LogItem log = createLogLine();
            log.PushBack( "tx_start_t_milli", String.valueOf(txBeginTime));
            log.PushBack("tx_end_t_milli", String.valueOf(txEndTime));
            log.PushBack("rq_enqueue_t_milli", String.valueOf(addInQueueTime));
            log.PushBack("rq_create_t_milli", String.valueOf(createTime));
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
            log.PushBack("tx_data_cnt", String.valueOf(transDataCnt));
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

    @Test
    public void startClient() throws IOException {
        Socket client = new Socket("127.0.0.1", 8438);
        client.setSoTimeout(80);
        PrintWriter output = new PrintWriter(client.getOutputStream(), true);
        String dataPath = "/media/song/test/data-set/beijing-traffic/TGraph/temporal.data1456550604218.data";
        BufferedReader br = new BufferedReader(new FileReader(dataPath));
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

//        for(int i=0;i<358616; i++) in.readLine();

        int queueSize;
        long lineSendCnt = 0;
        try {
            while (true) {
                try {
                    do {
                        String ss = in.readLine();
                        queueSize = Integer.parseInt(ss);
                        System.out.println(queueSize);
                    } while (queueSize > 180);
                }catch (SocketTimeoutException ignored){}

                for (int i = 0; i < 2 * transDataCnt; i++) {
                    String s = br.readLine();
                    if (s != null){
                        output.println(s);
                        lineSendCnt++;
                    }
                }
            }
        }catch (SocketException ignored){}
        System.out.println("send "+ lineSendCnt+" lines.");
    }
}
