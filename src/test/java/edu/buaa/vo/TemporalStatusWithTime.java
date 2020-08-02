package edu.buaa.vo;

/**
 * Created by song on 16-2-27.
 */
public class TemporalStatusWithTime{
    public final int time;
    public final String gridId;
    public final String chainId;
    public final Integer travelTime;
    public final Integer fullStatus;
    public final Integer vehicleCount;
    public final Integer segmentCount;

    public TemporalStatusWithTime(String line){
        String[] fields = line.split(":");
        time = Integer.valueOf(fields[0]);
        String[] ids = fields[1].split(",");
        gridId = ids[0];
        chainId = ids[1];
        String[] params = fields[2].split(",");
        travelTime = Integer.valueOf(params[0]);
        fullStatus = Integer.valueOf(params[1]);
        vehicleCount = Integer.valueOf(params[2]);
        segmentCount = Integer.valueOf(params[3]);
    }
}
