//package org.act.temporal.test.multiThread.clients;
//
//import org.act.tgraph.demo.client.vo.RoadChain;
//import org.act.tgraph.demo.client.vo.TemporalStatus;
//import org.act.tgraph.demo.client.Config;
//import org.act.temporal.test.utils.Helper;
//import org.act.temporal.test.utils.Monitor;
//import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.graphdb.Relationship;
//import org.neo4j.graphdb.Transaction;
//
//import java.io.*;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * Created by song on 16-2-26.
// *
// * used in read write test.
// */
//@Deprecated
//public class WriteOneLineClient extends Client {
//
//    private List<File> files;
//    private Map<Integer,BufferedReader> readers = new HashMap<>();
//    private Map<Integer,Integer> readIndex = new HashMap<>();
//    private List<Integer> timeList = new ArrayList<>();
//
//    @Deprecated
//    public WriteOneLineClient(Config config,List<File> fileList, GraphDatabaseService db) throws FileNotFoundException {
//        super(config);
//        this.files = fileList;
//        this.type = "write";
//        this.db = db;
//        this.files.sort(null);
//        for(File file:files){
//            int time = Helper.getFileTime(file);
//            readers.put(time, new BufferedReader(new FileReader(file)));
//            timeList.add(time);
//            readIndex.put(time, 0);
//        }
//    }
//
//
//
//    private void writeOneLinePerFile() throws IOException {
//        boolean modification=true;
//        while(modification) {
//            modification=false;
//            for (Integer time : timeList) {
//                BufferedReader br = readers.get(time);
//                int lineCount = readIndex.get(time);
//                String line = br.readLine();
//                if(line != null){
//                    if(lineCount == 0) {//not first line (header)
//                        modification = true;
//                    }else{
//                        TemporalStatus temporalStatus = new TemporalStatus(line);
//                        RoadChain roadChain = RoadChain.get(temporalStatus.gridId, temporalStatus.chainId);
//                        if (roadChain.getInNum() > 0 && roadChain.getOutNum() > 0) {
//                            Relationship relationship = null;
//                            monitor.snapShot(Monitor.BEGIN);
//                            try (Transaction tx = db.beginTx()) {
//                                relationship = roadChain.getRelationship(db);
//                                if (relationship != null) {
//                                    proxy.set(relationship, "travel-time", time, temporalStatus.getTravelTime());
//                                }
//                                tx.success();
//                            }
//                            if (relationship != null) {
//                                monitor.snapShot(Monitor.NEXT);
//                                try (Transaction tx = db.beginTx()) {
//                                    proxy.set(relationship, "full-status", time, temporalStatus.getFullStatus());
//                                    tx.success();
//                                }
//                                monitor.snapShot(Monitor.NEXT);
//                                try (Transaction tx = db.beginTx()) {
//                                    proxy.set(relationship, "vehicle-count", time, temporalStatus.getVehicleCount());
//                                    tx.success();
//                                }
//                                monitor.snapShot(Monitor.NEXT);
//                                try (Transaction tx = db.beginTx()) {
//                                    proxy.set(relationship, "segment-count", time, temporalStatus.getSegmentCount());
//                                    tx.success();
//                                }
//                                monitor.snapShot(Monitor.END);
//                            }
//                            modification = true;
//                        } else {
//                            System.out.println(roadChain);
//                        }
//                    }
//                    lineCount++;
//                    readIndex.put(time, lineCount);
//                }
//                if(this.exit){
//                    return;
//                }
//            }
//        }
//
//    }
//
//    @Override
//    public void run(){
//        currentThread().setName("Neo4jTemporalTest:"+this.getClass().getSimpleName()+"{"+currentThread().getId()+"}");
//        this.id = getId();
//        try{
//            writeOneLinePerFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }finally {
//            for(BufferedReader reader:readers.values()){
//                if(reader!=null) try {
//                    reader.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//}