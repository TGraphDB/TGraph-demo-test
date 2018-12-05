package org.act.temporal.test.utils;
import org.act.tgraph.demo.vo.Cross;
import org.act.tgraph.demo.vo.RelType;
import org.act.tgraph.demo.vo.RoadChain;
import org.act.tgraph.demo.vo.TemporalStatus;
import org.act.tgraph.demo.Config;
import org.act.tgraph.demo.driver.OperationProxy;
import org.act.tgraph.demo.driver.simulation.ArraySimulationProxy;
import org.act.temporal.test.vo.TemporalStatusWithTime;
import org.junit.After;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by song on 16-2-23.
 */
public class DataImportor {

    private Logger logger = LoggerFactory.getLogger(DataImportor.class);
    private Monitor monitor = new Monitor(logger);
    private Config config = new Config();
    private List<File> fileList;
    GraphDatabaseService db;
    @Test
    public void importTemporalData() throws IOException {
        Thread.currentThread().setName("Neo4jTemporal-importor");
        Helper.deleteExistDB(config);
        db= new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(config.dbPath )
                .loadPropertiesFromFile("")
                .newGraphDatabase();
        importNetwork(db);
        fileList= new ArrayList<>();
        Helper.getFileRecursive(new File(config.dataPathDir), fileList, 5);
        fileList.sort(null);
//        for(File f:fileList){
//            System.out.println(f.getName());
//        }
        int totalPartCount=1445;
        int nthPart=1;
        logger.info("split {} files into {} part,each part commit in one transaction", fileList.size(), totalPartCount);
        for(int i=nthPart;i<=totalPartCount;i++){
            logger.info("begin {}th part... {} files",i,fileList.size()/totalPartCount);
            importFileTemporalData(config.proxy , i, totalPartCount);
            logger.info("done");
        }
    }



    private void importFileTemporalData(OperationProxy proxy,int toBeImported, int totalPartCount) throws IOException {
        int[] tmp  = Helper.calcSplit(toBeImported, totalPartCount, fileList.size());
        int start = tmp[0];
        int end = tmp[1];
//        System.out.println("\n"+start+","+end);
//        System.exit(88);
        int totalCount=0;
        monitor.begin();
        try( Transaction tx = db.beginTx() ) {
            for (int i = start; i <= end; i++) {
                int time = Helper.getFileTime(fileList.get(i));
//                System.out.print(fileList.get(i).getName()+", importing...");
                try (BufferedReader br = new BufferedReader(new FileReader(fileList.get(i)))) {
                    String line;

                    for (int lineCount = 0; (line = br.readLine()) != null; lineCount++) {
//                        if(lineCount%1000==0)System.out.print(".");
                        if (lineCount == 0) continue;
                        TemporalStatus temporalStatus = new TemporalStatus(line);
                        RoadChain roadChain = RoadChain.get(temporalStatus.gridId, temporalStatus.chainId);
                        if (roadChain.getInNum() > 0 && roadChain.getOutNum() > 0) {
                            Relationship relationship = roadChain.getRelationship(db);
                            if (relationship != null) {
                                proxy.set(relationship, "travel-time", time, temporalStatus.getTravelTime());
                                proxy.set(relationship, "full-status", time, temporalStatus.getFullStatus());
                                proxy.set(relationship, "vehicle-count", time, temporalStatus.getVehicleCount());
                                proxy.set(relationship, "segment-count", time, temporalStatus.getSegmentCount());
                                totalCount+=4;
                            } else {
                                System.out.println(roadChain);
                            }
                        }
                    }

                }
//                System.out.println("done");
            }
            tx.success();
        }
        monitor.end(totalCount);
//        System.out.println("tx end");
    }

    private int OpCountPerTransaction = 28000;

    private void importOneFileTemporalData(OperationProxy proxy) throws IOException {
        long time_last=System.currentTimeMillis();
        boolean inc=true;

        long totalFileReadSize_last = 0;
        long totalSize_last=0;
        long totalLineCount_last = 0;
        long txCount_last=0;

        long totalFileReadSize = 0;
        long totalSize=0;
        long totalLineCount = 0;
        long txCount=0;

        try (BufferedReader br = new BufferedReader(new FileReader(config.dataPathFile),100000000)) {
            String line;
            boolean exit=false;
            while(!exit) {
                try (Transaction tx = db.beginTx()) {
                    for(int lineCount=0; !exit && lineCount< OpCountPerTransaction; lineCount++,totalLineCount++) {
                        if ((line = br.readLine()) != null) {
                            TemporalStatusWithTime t = new TemporalStatusWithTime(line);
                            RoadChain roadChain = RoadChain.get(t.gridId, t.chainId);
                            if (roadChain != null) {
                                Relationship relationship = roadChain.getRelationship(db);
                                if (relationship != null) {
                                    proxy.set(relationship, "travel-time", t.time, t.travelTime);
                                    proxy.set(relationship, "full-status", t.time, t.fullStatus);
                                    proxy.set(relationship, "vehicle-count", t.time, t.vehicleCount);
                                    proxy.set(relationship, "segment-count", t.time, t.segmentCount);
                                    totalSize += 64;
                                } else {
                                    logger.info(roadChain.toString());
                                }
                            } else {
                                logger.info("road chain{}:{} not found.", t.gridId, t.chainId);
                            }
                            totalFileReadSize+=line.length();
                        }else{
                            exit=true;
                            break;
                        }
                    }
//                    if(OpCountPerTransaction>100000000){
//                        inc=false;
//                    }else if(OpCountPerTransaction<1){
//                        inc=true;
//                    }
//
//                    if(inc){
//                        OpCountPerTransaction+=5;
//                    }else{
//                        OpCountPerTransaction-=5;
//                    }
                    tx.success();
                }
                txCount++;
                if(txCount%100==1){
                    long now = System.currentTimeMillis();
                    long interval = now - time_last;
                    logger.info("PROGRESS: {} % =======================",String.format("%.3f",totalLineCount*1000f/(14*10E8)));
                    logger.info("TOTAL: read {} line, read {} MB, write {} MB to DB, commit {} transactions",totalLineCount,totalFileReadSize/1024/1024,totalSize/1024/1024,txCount);
                    logger.info("AVG: in {}s: read {} line/s, read {} KB/s, write {} KB/s, commit {} tx/s  CURRENT: {} line/tx",interval/1000,
                            (totalLineCount-totalLineCount_last)*1000/interval,
                            ((totalFileReadSize-totalFileReadSize_last)*1000)/(1024*interval),
                            ((totalSize-totalSize_last)*1000/(1024*interval)),
//                            (txCount-txCount_last)*1000/interval);
                            String.format("%.3f", 100*1000f/interval),
                            OpCountPerTransaction);
                    time_last = now;
                    totalLineCount_last = totalLineCount;
                    totalFileReadSize_last = totalFileReadSize;
                    totalSize_last = totalSize;
                    txCount_last = txCount;
                }
            }
        }
    }

    @Test
    public void oneFileImporter() throws IOException {
        config.dbPath = Config.Default.dbPath+"-onefileimport";
        Thread.currentThread().setName("Neo4jTemporal-oneFile-importer");
        Helper.deleteExistDB(config);
        config.db= new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(config.dbPath )
                .loadPropertiesFromFile(config.neo4jConfigFile)
                .newGraphDatabase();
        db = config.db;
        importNetwork(config.db);
        importOneFileTemporalData(config.proxy);
        db.shutdown();
    }

    @Test
    public void arraySimulationInitImport() throws IOException {
        config.dbPath = Config.Default.dbPath+"-arraySimulationInit";
        Thread.currentThread().setName("Neo4jTemporal-oneFile-importer");
        Helper.deleteExistDB(config);
        config.db= new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(config.dbPath )
                .loadPropertiesFromFile(config.neo4jConfigFile)
                .newGraphDatabase();
        db = config.db;
        importNetwork(config.db);

        ArraySimulationProxy proxy = new ArraySimulationProxy();

        List<Relationship> rList = new ArrayList<>();

        try (Transaction tx = db.beginTx()) {
            for(Relationship relationship: GlobalGraphOperations.at(db).getAllRelationships()){
                rList.add(relationship);
            }
        }

        Monitor monitor = new Monitor(config.logger);
        monitor.begin();
        for(int i=0;i<rList.size();i++) {
            Relationship relationship = rList.get(i);
            try (Transaction tx = db.beginTx()) {
                proxy.initImport(relationship, "travel-time", 11700);
                proxy.initImport(relationship, "full-status", 11700);
                proxy.initImport(relationship, "vehicle-count", 11700);
                proxy.initImport(relationship, "segment-count", 11700);
                tx.success();
            }
            if(i%100==1){
                monitor.end(i);
                monitor.begin();
            }
        }
        monitor.end(rList.size()%100);
        db.shutdown();
    }

    @Test
    public void arraySimulationInitImport2() throws IOException {
        config.dbPath = Config.Default.dbPath+"-arraySimulationInit";
        Thread.currentThread().setName("Neo4jTemporal-oneFile-importer");
        Helper.deleteExistDB(config);
        List<RoadChain> roadChainList = new ArrayList<>();

        BatchInserter inserter = null;
        try
        {
            Map<String, Object> nodeProperties = new HashMap<>();
            int len=12000;
            int[] v = new int[len*2];
            for(int i=0;i<v.length;i+=2){
                v[i]=len-i;
                v[i+1]=len-i;
            }
            Map<String, Object> edgeProperties = new HashMap<>();
            edgeProperties.put("travel-time",v);
            edgeProperties.put("full-status",v);
            edgeProperties.put("vehicle-count",v);
            edgeProperties.put("segment-count",v);

            inserter = BatchInserters.inserter(config.dbPath);
            try (BufferedReader br = new BufferedReader(new FileReader(Config.Default.dataPathNetwork))) {
                String line;
                for (int lineCount = 0;(line = br.readLine()) != null;lineCount++) {
                    if(lineCount==0)continue;
//                System.out.println(line);
                    roadChainList.add(new RoadChain(line));
//                if(lineCount>5)break;
                }
                System.out.println("roadChainListSize:"+roadChainList.size());
                int emptyInCount=0;
                int emptyOutCount=0;
                int emptyAllCount=0;
                for (int i=0;i<roadChainList.size();i++){
                    RoadChain roadChain = roadChainList.get(i);
                    roadChain.updateNeighbors();
                    int inCount = roadChain.getInNum();
                    int outCount = roadChain.getOutNum();
                    if(inCount==0 && outCount==0){
                        emptyAllCount++;
                    }else if(inCount==0 && outCount>0){
                        emptyInCount++;
                    }else if(inCount>0 && outCount==0){
                        emptyOutCount++;
                    } else {
                        long inNode = inserter.createNode(nodeProperties);
                        long outNode = inserter.createNode(nodeProperties);
                        inserter.createRelationship( inNode, outNode, RelType.ROAD_TO, edgeProperties);
                    }
                }
                System.out.println("borderCount:empty in("+emptyInCount+"),empty out("+emptyOutCount+"),empty all("+emptyAllCount+")");
                System.out.println("Total cross count:"+Cross.getTotalCount());
            }
        }
        finally
        {
            if ( inserter != null )
            {
                inserter.shutdown();
            }
        }
    }


    public static void importNetwork(GraphDatabaseService db) throws IOException {

        List<RoadChain> roadChainList = new ArrayList<>();
//        System.exit(0);
        try( Transaction tx = db.beginTx() )
        {
            try (BufferedReader br = new BufferedReader(new FileReader(Config.Default.dataPathNetwork))) {
                String line;
                for (int lineCount = 0;(line = br.readLine()) != null;lineCount++) {
                    if(lineCount==0)continue;
//                System.out.println(line);
                    roadChainList.add(new RoadChain(line));
//                if(lineCount>5)break;
                }
                System.out.println("roadChainListSize:"+roadChainList.size());
                int emptyInCount=0;
                int emptyOutCount=0;
                int emptyAllCount=0;
                for (int i=0;i<roadChainList.size();i++){
                    RoadChain roadChain = roadChainList.get(i);
                    roadChain.updateNeighbors();
                    int inCount = roadChain.getInNum();
                    int outCount = roadChain.getOutNum();
                    if(inCount==0 && outCount==0){
                        emptyAllCount++;
                    }else if(inCount==0 && outCount>0){
                        emptyInCount++;
                    }else if(inCount>0 && outCount==0){
                        emptyOutCount++;
                    }else{
                        Cross inCross = Cross.getStartCross(roadChain);
                        Cross outCross = Cross.getEndCross(roadChain);
                        Node inNode = db.createNode();
                        Node outNode = db.createNode();
                        inCross.setNode(inNode);
                        outCross.setNode(outNode);
                        Relationship relationship = inNode.createRelationshipTo(outNode, RelType.ROAD_TO);
                        roadChain.setRelationship(relationship);
                    }
//                if(i>5)break;
//                System.out.println(roadChainList.get(i));
                }
//            System.out.println(RoadChain.relationCount);
                System.out.println("borderCount:empty in("+emptyInCount+"),empty out("+emptyOutCount+"),empty all("+emptyAllCount+")");
//                System.out.println(crossList.size());
                System.out.println("Total cross count:"+Cross.getTotalCount());
            }


            tx.success();
        }
    }
    @After
    public void close(){
        if(db!=null){db.shutdown();}
    }
}
