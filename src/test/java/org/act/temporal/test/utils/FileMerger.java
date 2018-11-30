package org.act.temporal.test.utils;

import org.act.tgraph.demo.vo.RoadChain;
import org.act.tgraph.demo.vo.TemporalStatus;
import org.act.tgraph.demo.Config;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by song on 16-2-26.
 */
public class FileMerger {

    private Config config = new Config();

    public void readNetworkFile() throws IOException {
        int lineCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(config.dataPathNetwork))) {
            String line;
            while ((line = br.readLine()) != null) {
                if(lineCount>0){
                    new RoadChain(line);
                }
                lineCount++;
            }
        }
        System.out.println("roadChainListSize:" + lineCount);
    }
    @Test
    public void mergeInOne() throws IOException {
        config.dataPathDir="D:\\java_project\\data\\北京道路数据";
        config.dataPathFile = config.Default.dataPathFile+System.currentTimeMillis()+".data";
        Logger logger = config.logger;

        readNetworkFile();

        ArrayList<File> fileList= new ArrayList<>();
        Helper.getFileRecursive(new File(config.dataPathDir), fileList, 15);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return -(o2.getName().compareTo(o1.getName()));
            }
        });
        for(int i=1;i<fileList.size();i++){
//            System.out.println(fileList.get(i).getName());
            if(fileList.get(i).getName().equals(fileList.get(i-1).getName()))break;
        }
        System.out.println(fileList.size());

        long totalLine=0;
        long skipLine = 0;
        long timestamp=System.currentTimeMillis();
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(config.dataPathFile),20000)) {
            for (int i = 0; i < fileList.size(); i++) {
                int time = Helper.getFileTime(fileList.get(i));
                try (BufferedReader br = new BufferedReader(new FileReader(fileList.get(i)))) {
                    String line;
                    if(i%400==1){
                        long now = System.currentTimeMillis();
                        logger.info("write {} lines, skip {} lines", totalLine, skipLine);
                        logger.info("read {} files, current:{}",i,fileList.get(i).getName());
                        logger.info("speed:{} files/s, {} lines/s",i*1000/(now-timestamp),totalLine*1000/(now-timestamp));
                        timestamp = now;
                    }
                    for (int lineCount = 0; (line = br.readLine()) != null; lineCount++) {
                        if (lineCount == 0) continue;
                        TemporalStatus temporalStatus = new TemporalStatus(line);
                        RoadChain roadChain = RoadChain.get(temporalStatus.gridId, temporalStatus.chainId);
                        if (roadChain!=null && roadChain.getInNum() > 0 && roadChain.getOutNum() > 0) {
                            writer.append(String.valueOf(time)).append(':')
                                    .append(temporalStatus.gridId).append(',')
                                    .append(temporalStatus.chainId).append(':')
                                    .append(temporalStatus.getTravelTime()+",")
                                    .append(temporalStatus.getFullStatus()+",")
                                    .append(temporalStatus.getVehicleCount()+",")
                                    .append(temporalStatus.getSegmentCount()+"");
                            writer.newLine();
                            totalLine++;
                        }else{
                            skipLine++;
                        }
                    }
                }
            }
        }
    }
}
