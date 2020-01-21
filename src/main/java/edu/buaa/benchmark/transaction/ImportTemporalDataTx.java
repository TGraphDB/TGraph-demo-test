package edu.buaa.benchmark.transaction;

import java.util.List;

public class ImportTemporalDataTx extends AbstractTransaction {
    public List<StatusUpdate> data;
    public ImportTemporalDataTx(){}
    public ImportTemporalDataTx(List<StatusUpdate> lines) {
        this.setTxType(TxType.tx_import_temporal_data);
        this.data = lines;
        Metrics m = new Metrics();
        m.setReqSize(lines.size());
        this.setMetrics(m);
    }

    public List<StatusUpdate> getData() {
        return data;
    }

    public void setData(List<StatusUpdate> data) {
        this.data = data;
    }

    public static class StatusUpdate{
        private long roadId;
        private int time;
        private int travelTime;
        private int jamStatus;
        private int segmentCount;

        public StatusUpdate(long roadId, int time, int travelTime, int jamStatus, int segmentCount) {
            this.roadId = roadId;
            this.time = time;
            this.travelTime = travelTime;
            this.jamStatus = jamStatus;
            this.segmentCount = segmentCount;
        }

        public StatusUpdate(){}

        public long getRoadId() {
            return roadId;
        }

        public int getTime() {
            return time;
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

        public void setTime(int time) {
            this.time = time;
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
}
