package edu.buaa.benchmark.transaction;

public abstract class AbstractTransaction {
    public enum TxType{
        tx_import_static_data,
        tx_import_temporal_data,
        tx_query_reachable_area,
        tx_query_road_earliest_arrive_time_aggr
    }

    private TxType txType;
    private Metrics metrics;
    private Result result;

    public TxType getTxType() {
        return txType;
    }

    public void setTxType(TxType txType) {
        this.txType = txType;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public void validateResult(Result result){}

    public static class Metrics{
        private int waitTime; // duration, in milliseconds
        private long sendTime; // timestamp, in milliseconds
        private int exeTime; // duration, in milliseconds
        private int connId;
        private int reqSize; // user defined value, maybe bytes or rows
        private int returnSize; // user defined value, maybe bytes or rows

        public int getExeTime() {
            return exeTime;
        }

        public void setExeTime(int exeTime) {
            this.exeTime = exeTime;
        }

        public int getWaitTime() {
            return waitTime;
        }

        public void setWaitTime(int waitTime) {
            this.waitTime = waitTime;
        }

        public long getSendTime() {
            return sendTime;
        }

        public void setSendTime(long sendTime) {
            this.sendTime = sendTime;
        }

        public int getConnId() {
            return connId;
        }

        public void setConnId(int connId) {
            this.connId = connId;
        }

        public int getReqSize() {
            return reqSize;
        }

        public void setReqSize(int reqSize) {
            this.reqSize = reqSize;
        }

        public int getReturnSize() {
            return returnSize;
        }

        public void setReturnSize(int returnSize) {
            this.returnSize = returnSize;
        }
    }

    public static class Result{

    }
}
