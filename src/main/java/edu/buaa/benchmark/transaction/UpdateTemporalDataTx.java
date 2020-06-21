package edu.buaa.benchmark.transaction;

import java.util.List;

public class UpdateTemporalDataTx extends AbstractTransaction {

    private long roadId;
    private int startTime;
    private int endTime;
    private int travelTime;
    private int jamStatus;
    private int segmentCount;
    public UpdateTemporalDataTx(){}
    public UpdateTemporalDataTx(long roadId, int startTime, int endTime, int travelTime, int jamStatus, int segmentCount) {
        this.roadId = roadId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.travelTime = travelTime;
        this.jamStatus = jamStatus;
        this.segmentCount = segmentCount;
    }

    public UpdateTemporalDataTx(long roadId, int startTime, int travelTime, int jamStatus, int segmentCount) {
        this.roadId = roadId;
        this.startTime = startTime;
        this.endTime = startTime;
        this.travelTime = travelTime;
        this.jamStatus = jamStatus;
        this.segmentCount = segmentCount;
    }

    public long getRoadId() {
        return roadId;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public int getTravelTime() {
        return travelTime;
    }

    public int getJamStatus() {
        return jamStatus;
    }

    public int getSegmentCount() {
        return segmentCount;
    }

    public void setRoadId(long roadId) {
        this.roadId = roadId;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public void setTravelTime(int travelTime) {
        this.travelTime = travelTime;
    }

    public void setJamStatus(int jamStatus) {
        this.jamStatus = jamStatus;
    }

    public void setSegmentCount(int segmentCount) {
        this.segmentCount = segmentCount;
    }


}
