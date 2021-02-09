package edu.buaa.multiThread.clients;

import edu.buaa.client.vo.RoadChain;
import edu.buaa.client.vo.TemporalStatus;
import edu.buaa.utils.Helper;
import edu.buaa.vo.Line;

import java.io.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created by song on 16-2-26.
 */
public class ReadFileServer extends Thread{
    private List<File> fileList;
    private BlockingQueue<Line> full_status_queue;
    private BlockingQueue<Line> travel_time_queue;
    private BlockingQueue<Line> segment_count_queue;
    private BlockingQueue<Line> vehicle_count_queue;

    public ReadFileServer(List<File> fileList, BlockingQueue<Line> full_status_queue, BlockingQueue<Line> travel_time_queue, BlockingQueue<Line> segment_count_queue, BlockingQueue<Line> vehicle_count_queue) {
        this.fileList = fileList;
        this.full_status_queue = full_status_queue;
        this.travel_time_queue = travel_time_queue;
        this.segment_count_queue = segment_count_queue;
        this.vehicle_count_queue = vehicle_count_queue;
    }

    @Override
    public void run(){
        currentThread().setName("Neo4jTemporalTest:"+this.getClass().getSimpleName()+"{"+currentThread().getId()+"}");
        for(File file:fileList){
            int time = Helper.getFileTime(file);
            try(BufferedReader reader = new BufferedReader(new FileReader(file),2000000)){
                String line;
                boolean isHeader=true;
                while ((line= reader.readLine())!=null){
                    if(isHeader){
                        isHeader=false;
                    }else {
                        TemporalStatus temporalStatus = new TemporalStatus(line);
                        RoadChain roadChain = RoadChain.get(temporalStatus.gridId, temporalStatus.chainId);
                        if (roadChain.getInNum() > 0 && roadChain.getOutNum() > 0) {
                            full_status_queue.put(new Line(time, roadChain, temporalStatus.getFullStatus()));
                            travel_time_queue.put(new Line(time, roadChain, temporalStatus.getTravelTime()));
                            segment_count_queue.put(new Line(time, roadChain, temporalStatus.getSegmentCount()));
                            vehicle_count_queue.put(new Line(time, roadChain, temporalStatus.getVehicleCount()));
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Line exit = new Line(0,"exit");
        exit.exit=true;
        try {
            full_status_queue.put(exit);
            travel_time_queue.put(exit);
            segment_count_queue.put(exit);
            vehicle_count_queue.put(exit);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}