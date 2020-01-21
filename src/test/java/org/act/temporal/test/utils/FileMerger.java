package org.act.temporal.test.utils;

import edu.buaa.client.vo.RoadChain;
import edu.buaa.client.vo.TemporalStatus;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by song on 16-2-26.
 */
public class FileMerger {
    String dataPathDir="D:\\java_project\\data\\北京道路数据";
    String dataPathNetwork = "D:\\java_project\\data\\北京道路数据\\Topo.csv";
    String dataPathFile = "D:\\java_project\\data\\北京道路数据\\merged."+System.currentTimeMillis()+".data";
    Logger logger = LoggerFactory.getLogger(FileMerger.class);

    public void readNetworkFile() throws IOException {
        int lineCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(dataPathNetwork))) {
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
        readNetworkFile();

        ArrayList<File> fileList= new ArrayList<>();
        Helper.getFileRecursive(new File(dataPathDir), fileList, 15);
        fileList.sort(Comparator.comparing(File::getName));
        for(int i=1;i<fileList.size();i++){
//            System.out.println(fileList.get(i).getName());
            if(fileList.get(i).getName().equals(fileList.get(i-1).getName()))break;
        }
        System.out.println(fileList.size());

        long totalLine=0;
        long skipLine = 0;
        long timestamp=System.currentTimeMillis();
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(dataPathFile),20000)) {
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
