//package edu.buaa.client;
//
//import edu.buaa.utils.Helper;
//import edu.buaa.utils.TransactionWrapper;
//import edu.buaa.client.vo.Cross;
//import edu.buaa.client.vo.RelType;
//import edu.buaa.client.vo.RoadChain;
//import edu.buaa.client.vo.TemporalStatus;
//import org.neo4j.graphdb.*;
//import org.neo4j.graphdb.factory.GraphDatabaseFactory;
//import org.neo4j.temporal.TimePoint;
//import org.neo4j.tooling.GlobalGraphOperations;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Created by song on 16-1-22.
// */
//public class DataImport {
//    private static Config config = new Config();
//    private static Logger logger = LoggerFactory.getLogger("");
//    static GraphDatabaseService db;
//
//    private static void cleanFolder(File dir){
//        if (!dir.exists()){
//            dir.mkdir();
//        }else{
//            Helper.deleteAllFilesOfDir( dir );
//            dir.mkdir();
//        }
//    }
//
//    public static void main(String[] args) throws IOException {
//        //System.out.println(System.getProperty("java.home"));
////        config.dbPath += "-more-data";
////        File dir = new File(config.dbPath);
////        System.out.println(dir.getAbsolutePath());
////
////        cleanFolder(dir);
////
////        importNetwork();
//////        checkNetConstrain();
////        importTemporalData(); // data from 2010-11-04 00:00(1288800000) to 2010-11-07 10:47(1289098020)
//////        checkConstrain();
////        if(db!=null)db.shutdown();
////        System.exit(0);
//    }
//
//
//    private static void importTemporalData(File dataPathDir) throws IOException {
//        List<File> fileList = new ArrayList<>();
//        Helper.getFileRecursive(dataPathDir, fileList, 5);
//        fileList.sort(null);
////        System.out.println(fileList.get(0).getName() + " " + fileList.get(fileList.size() - 1).getName());
////        int t = Helper.timeStr2int(fileList.get(0).getName().substring(9, 21));
////        int t1 = Helper.timeStr2int(fileList.get(fileList.size()-1).getName().substring(9, 21));
////        System.out.println(t+" "+t1);// 1288541160 1289231700
////        for (int i = 0; i < fileList.size(); i++) {
////            System.out.println(fileList.get(i).getName());
////        }
//
//        for (int i = 0; i < 1000; i++) {
//            try( Transaction tx = db.beginTx() ) {
//                int time = Helper.timeStr2int(fileList.get(i).getName().substring(9, 21));
//                System.out.println(fileList.get(i).getName()+ " "+ time);
//                try (BufferedReader br = new BufferedReader(new FileReader(fileList.get(i)))) {
//                    String line;
//                    for (int lineCount = 0; (line = br.readLine()) != null; lineCount++) {
//                        if (lineCount == 0) continue;
//                        TemporalStatus temporalStatus = new TemporalStatus( line);
//                        RoadChain roadChain = RoadChain.get( temporalStatus.gridId, temporalStatus.chainId );
//                        if (roadChain.getInNum() > 0 || roadChain.getOutNum() > 0) {
//                            Relationship relationship = roadChain.getRelationship(db);
//                            if (relationship != null) {
//                                relationship.setTemporalProperty("travel-time", new TimePoint(time), temporalStatus.getTravelTime());
//                                relationship.setTemporalProperty("full-status", new TimePoint(time), temporalStatus.getFullStatus());
//                                relationship.setTemporalProperty("vehicle-count", new TimePoint(time), temporalStatus.getVehicleCount());
//                                relationship.setTemporalProperty("segment-count", new TimePoint(time), temporalStatus.getSegmentCount());
//                                int minT = (Integer) relationship.getProperty("min-time");
//                                int maxT = (Integer) relationship.getProperty("max-time");
//                                int dataCount = (Integer) relationship.getProperty("data-count");
//                                if(minT>time) relationship.setProperty("min-time",time);
//                                if(maxT<time) relationship.setProperty("max-time",time);
//                                relationship.setProperty("data-count",dataCount+1);
//                                relationship.setTemporalProperty("temporal-point", new TimePoint(dataCount+1), time);
//                            } else {
//                                System.out.println(roadChain);
//                            }
//                        } else {
//                            continue;
//                        }
//                    }
//                }
//                tx.success();
//            }
//        }
//    }
//
//    public static void initDB(){
//        if(db==null) {
//            db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(config.dbPath));
//        }
//    }
//
//    public static void importNetwork() throws IOException {
//
//        List<RoadChain> roadChainList = new ArrayList<>();
//
//
//        try (BufferedReader br = new BufferedReader(new FileReader(config.dataPathNetwork))) {
//            String line;
//            for (int lineCount = 0; (line = br.readLine()) != null; lineCount++) {
//                if (lineCount == 0) continue;//ignore headers
//                try {
//                    roadChainList.add(new RoadChain(line, lineCount));
//                }catch (RuntimeException e){
//                    System.out.println(e.getMessage()+" at line:"+lineCount);
//                }
//            }
//        }
////        //extra check if has invalid constrains
//        for(RoadChain roadChain: roadChainList){
//            roadChain.updateNeighbors();
//        }
////        System.exit(0);
//
//        initDB();
//
//        System.out.println("roadChainListSize:" + roadChainList.size());
//        int emptyInCount = 0;
//        int emptyOutCount = 0;
//        int emptyAllCount = 0;
//        int normalRoadCount=0;
//
//        int totalPartCount=100;
//        for(int ith=1; ith<=totalPartCount; ith++){
//            int[] tmp = Helper.calcSplit(ith,totalPartCount,roadChainList.size());
////            System.out.println(ith+": from "+tmp[0]+" to "+tmp[1]);
//            try (Transaction tx = db.beginTx()) {
//                for (int i = tmp[0]; i <= tmp[1]; i++) {
//                    RoadChain roadChain = roadChainList.get(i);
//                    int inCount = roadChain.getInNum();
//                    int outCount = roadChain.getOutNum();
//                    if (inCount == 0 && outCount == 0) {
//                        emptyAllCount++;
//                    } else{
//                        if (inCount == 0 && outCount > 0) {
//                            emptyInCount++;
//                        } else if (inCount > 0 && outCount == 0) {
//                            emptyOutCount++;
//                        }
//                        normalRoadCount++;
//                        Cross inCross = Cross.getStartCross( roadChain );
//                        Cross outCross = Cross.getEndCross(roadChain);
//                        Node inNode, outNode;
//                        if (inCross.getNode(db) == null) {
//                            inNode = db.createNode();
//                            inCross.setNode(inNode);
//                            inNode.setProperty("cross-id", inCross.id);
//                        } else {
//                            inNode = inCross.getNode(db);
//                        }
//                        if (outCross.getNode(db) == null) {
//                            outNode = db.createNode();
//                            outCross.setNode(outNode);
//                            outNode.setProperty("cross-id", outCross.id);
//                        } else {
//                            outNode = outCross.getNode(db);
//                        }
//
//                        Relationship relationship = inNode.createRelationshipTo(outNode, RelType.ROAD_TO);
//                        relationship.setProperty("uid", roadChain.getUid());
////                        relationship.setProperty("line-num", roadChain.getLineNum());
//                        relationship.setProperty("grid-id", roadChain.getGridId());
//                        relationship.setProperty("chain-id", roadChain.getChainId());
//                        relationship.setProperty("type", roadChain.getType());
//                        relationship.setProperty("length", roadChain.getLength());
//                        relationship.setProperty("angle", roadChain.getAngle());
//                        relationship.setProperty("in-count", roadChain.getInNum());
//                        relationship.setProperty("out-count", roadChain.getOutNum());
//                        relationship.setProperty("in-roads", roadChain.briefInChain());
//                        relationship.setProperty("out-roads", roadChain.briefOutChain());
//                        relationship.setProperty("data-count",0);
//                        relationship.setProperty("min-time",Integer.MAX_VALUE);
//                        relationship.setProperty("max-time",0);
//                        roadChain.setRelationship(relationship);
//                    }
//                }
//                tx.success();
//            }
//        }
////      System.out.println(RoadChain.relationCount);
//        System.out.println("borderCount:empty in("+emptyInCount+"),empty out("+emptyOutCount+"),empty all("+emptyAllCount+"),normal("+normalRoadCount+")");
////      System.out.println(crossList.size());
//        System.out.println("Total cross count:" + Cross.getTotalCount());
//    }
//
//    public static void checkConstrain(){
//        initDB();
//        new TransactionWrapper<Object>(){
//            @Override
//            public void runInTransaction() {
//                int noDataEdgeCount=0;
//                for(Relationship r: GlobalGraphOperations.at(db).getAllRelationships()){
//
//                    boolean hasData=false;
//                    for(int i=1288541160+40;i<1289231700+100;i+=3600*24){
//                        if(r.getTemporalProperty("vehicle-count",new TimePoint(i))!=null){
//                            hasData=true;break;
//                        }
//                    }
//                    if(!hasData){
//                        noDataEdgeCount++;
//                    }else{
//
//                    }
//                }
//                logger.info("edge has no dyn data: {}", noDataEdgeCount);//19021 edges.
////                Node node = db.getNodeById(89111);
////                System.out.println(node.getProperty("cross-id"));
////                for(Relationship r:node.getRelationships(Direction.INCOMING)){
////                    Node neighbor = r.getOtherNode(node);
////                    logger.info("{}-->{}, in:{} out:{} vehicle-count:{}",
////                            neighbor.getId(),node.getId(),
////                            r.getProperty("in-count"),
////                            r.getProperty("out-count"),
////                            r.getDynPropertyPointValue("vehicle-count",1288541160+4500));
////                }
////                logger.info("==================================");
////                for(Relationship r:node.getRelationships(Direction.OUTGOING)){
////                    Node neighbor = r.getOtherNode(node);
////                    logger.info("{}-->{}, in:{} out:{}",
////                            node.getId(),neighbor.getId(),
////                            r.getProperty("in-count"),
////                            r.getProperty("out-count"));
////                }
////                logger.info("==================================");
////                for(Relationship r:node.getRelationships(Direction.BOTH)){
////                    Node neighbor = r.getOtherNode(node);
////                    if(neighbor.getId()==r.getEndNode().getId()) {
////                        logger.info("{}-->{}, in:{} out:{}",
////                                node.getId(), neighbor.getId(),
////                                r.getProperty("in-count"),
////                                r.getProperty("out-count"));
////                    }else{
////                        logger.info("{}-->{}, in:{} out:{}",
////                                neighbor.getId(),node.getId(),
////                                r.getProperty("in-count"),
////                                r.getProperty("out-count"));
////                    }
////                }
//            }
//        }.start(db);
//    }
//
//    private static void checkNetConstrain() {
//        initDB();
//        new TransactionWrapper<Object>() {
//            @Override
//            public void runInTransaction() {
//
//            }
//        }.start(db);
//    }
//}
//
//
////            int emptyInCount=0;
////            int emptyOutCount=0;
////            int emptyAllCount=0;
////            for (int i=0;i<roadChainList.size();i++) {
////                RoadChain roadChain = roadChainList.get(i);
////                //roadChain.updateNeighbors();
////                int inCount = roadChain.getInNum();
////                int outCount = roadChain.getOutNum();
////                if (inCount == 0 && outCount == 0) {
////                    emptyAllCount++;
////                } else if (inCount == 0 && outCount > 0) {
////                    emptyInCount++;
////                } else if (inCount > 0 && outCount == 0) {
////                    emptyOutCount++;
////                } else {
////                    Cross inCross = Cross.getStartCross(roadChain);
////                    Cross outCross = Cross.getEndCross(roadChain);
////                    Node inNode = inCross.getNode();
////                    Node outNode = outCross.getNode();
////                    if(inNode==null && outNode==null){
////                        System.out.println(inCross.id+" "+outCross.id);
////                    }else if(inNode==null && outNode!=null){
////                        System.out.println(inCross.id+" "+outCross.id);
////                    }else if(inNode!=null && outNode==null){
////                        System.out.println(inCross.id+" "+outCross.id);
////                    }else {
////                        try {
////                            inNode.createRelationshipTo(outNode, RelType.ROAD_TO);
////                        } catch (NullPointerException e) {
////
////                        }
////                    }
////                }
////            }
////            int edgeCount1=0;
////            int edgeCount=0;
////            int nodeCount=0;
////            for(Cross cross:Cross.crossMap.values()){
////                Node node = db.createNode();
////                node.setProperty("cross-id", cross.id);
////                cross.setNode(node);
////                nodeCount++;
////            }
//////            for(Cross cross:crossList) {
//////
//////            }
////            int emptyInCount=0;
////            int emptyOutCount=0;
////            int emptyAllCount=0;
////            for(RoadChain roadChain:roadChainList){
////                if((roadChain.inChains.size()>0) && (roadChain.outChains.size()>0)){
////                    long inNodeId = Cross.getStartCross(roadChain).getNodeId();
////                    long outNodeId = Cross.getEndCross(roadChain).getNodeId();
////                    if(inNodeId==0 && outNodeId==0){
////                        emptyAllCount++;
////                    }else if(inNodeId==0 && outNodeId>0){
////                        emptyInCount++;
////                    }else if(inNodeId>0 && outNodeId==0){
////                        emptyOutCount++;
////                    }else {
////                        Node inNode = Cross.getStartCross(roadChain).getNode();
////                        Node outNode = Cross.getEndCross(roadChain).getNode();
////                        edgeCount1++;
////                        if (inNode != null && outNode != null) {
////                            inNode.createRelationshipTo(outNode, RelType.ROAD_TO);
////                            edgeCount++;
////                        }
////                    }
////                }
////            }
////            System.out.println("empty in(" + emptyInCount + "),empty out(" + emptyOutCount + "),empty all(" + emptyAllCount + ")");
////
////            System.out.println("nodeCount:" + nodeCount);
////            System.out.println("edgeCount:" + edgeCount);
////            System.out.println("edgeCount1:" + edgeCount1);
//
//
//
//
////                    Integer uid = (Integer) r.getProperty("uid");
////                    Integer lineNum = (Integer) r.getProperty("line-num");
////                    if(uid!=null && lineNum!=null) {
////                        if (((int) uid) != ((int) lineNum)) {
////                            logger.warn("uid{} not equals lineNum{}!", uid, lineNum);
////                            return;
////                        }
////                    }