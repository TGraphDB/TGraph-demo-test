package org.act.temporal.test.multiThread.clients;

import org.act.tgraph.demo.client.vo.RoadChain;
import org.act.tgraph.demo.client.vo.TemporalStatus;
import org.act.tgraph.demo.client.Config;
import org.act.tgraph.demo.client.driver.OperationProxy;
import org.act.temporal.test.utils.Helper;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.io.*;
import java.util.*;

/**
 * Created by song on 16-2-26.
 * once used in write test.
 */
@Deprecated
public class MultiFileWriteClient extends Thread{

    private final OperationProxy proxy;
    private final GraphDatabaseService db;
    private List<File> files;
    private Map<Integer,BufferedReader> readers = new HashMap<>();
    private List<Integer> timeList = new ArrayList<>();
    private Map<Integer,Integer> readIndex = new HashMap<>();
    private Map<Integer,Boolean> fileReadEnds = new HashMap<>();

    @Deprecated
    public MultiFileWriteClient(Config config,List<File> fileList, GraphDatabaseService db) throws FileNotFoundException {
        this.files = fileList;
        this.proxy = config.proxy;
        this.db = db;
        this.files.sort(null);
        for(File file:files){
            int time = Helper.getFileTime(file);
            readers.put(time,new BufferedReader(new FileReader(file)));
            timeList.add(time);
            readIndex.put(time, 0);
            fileReadEnds.put(time,false);
        }
    }



    private void writeMultiPartFileInOneTransaction() throws IOException {
        boolean modification=true;
        while(modification) {
            modification=false;
            try (Transaction tx = db.beginTx()) {
                //                ArrayList toRemove = new ArrayList();
                for (Integer time : timeList) {
                    if (!fileReadEnds.get(time)) {
                        BufferedReader br = readers.get(time);
                        int lineCount = readIndex.get(time);
                        int targetLineCount = lineCount + 100;
                        String line;
                        while ((line = br.readLine()) != null && (lineCount < targetLineCount)) {
                            if (lineCount > 0) {//not first line (header)
                                TemporalStatus temporalStatus = new TemporalStatus(line);
                                RoadChain roadChain = RoadChain.get(temporalStatus.gridId, temporalStatus.chainId);
                                if (roadChain.getInNum() > 0 && roadChain.getOutNum() > 0) {
                                    Relationship relationship = roadChain.getRelationship(db);
                                    if (relationship != null) {
                                        proxy.set(relationship, "travel-time", time, temporalStatus.getTravelTime());
                                        proxy.set(relationship, "full-status", time, temporalStatus.getFullStatus());
                                        proxy.set(relationship, "vehicle-count", time, temporalStatus.getVehicleCount());
                                        proxy.set(relationship, "segment-count", time, temporalStatus.getSegmentCount());
                                        modification=true;
                                    } else {
                                        System.out.println(roadChain);
                                    }
                                }
                            }
                            lineCount++;
                        }
                        if (lineCount == targetLineCount) {
                            readIndex.put(time, lineCount);
                        } else {//reach end
                            fileReadEnds.put(time, true);
                        }
                    }
                }
                tx.success();
            }
        }
        for(BufferedReader reader:readers.values()){
            reader.close();
        }
    }

    public void writeOne(){}
    public void readOne(){}


    @Override
    public void run(){
        try{
            writeMultiPartFileInOneTransaction();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void go(){
        this.start();
    }
}