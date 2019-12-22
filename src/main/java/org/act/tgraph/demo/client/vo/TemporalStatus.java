package org.act.tgraph.demo.client.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by song on 16-1-25.
 *
 * correspond to one line in TJamData_YYYYMMDDHHmm.csv (for example: TJamData_201011010036.csv)
 */
public class TemporalStatus {
    private final Integer travelTime;
    private final Integer fullStatus;
    private final Integer vehicleCount;
    private final Integer segmentCount;
    private int index;
    public final String gridId;
    public final String chainId;
    private int length;
    private int type;
    public List<RoadSegment> segments = new ArrayList<>();
    public TemporalStatus(String line) {
        String[] fields = line.split(",");
        int index = Integer.valueOf(fields[0]);
        gridId = fields[1];
        chainId = fields[2];
        int ignore = Integer.valueOf(fields[3]);
        type = Integer.valueOf(fields[4]);
        length = Integer.valueOf(fields[5]);
        travelTime = Integer.valueOf(fields[6]);
        fullStatus = Integer.valueOf(fields[7]);
        vehicleCount = Integer.valueOf(fields[8]);
        segmentCount = Integer.valueOf(fields[9]);
        for(int i=0;i<segmentCount;i++){
            segments.add(new RoadSegment(
                    Integer.valueOf(fields[10+i*4]),
                    Integer.valueOf(fields[11+i*4]),
                    Integer.valueOf(fields[12+i*4]),
                    Integer.valueOf(fields[13+i*4])
            ));
        }
    }

    public Integer getTravelTime() {
        return travelTime;
    }

    public Integer getFullStatus() {
        return fullStatus;
    }

    public Integer getVehicleCount() {
        return vehicleCount;
    }

    public Integer getSegmentCount(){
        return segmentCount;
    }

    public static class RoadSegment{
        public final int distanceToEnd;
        public final int length;
        public final int travelTime;
        public final int fullStatus;

        public RoadSegment(int distanceToEnd, int length, int fullStatus, int travelTime) {
            this.distanceToEnd = distanceToEnd;
            this.length = length;
            this.travelTime = travelTime;
            this.fullStatus = fullStatus;
        }
    }


}
