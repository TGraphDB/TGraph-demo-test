package edu.buaa.benchmark.transaction;

public abstract class AbstractTransaction {
    public enum TxType{
        tx_index_tgraph_aggr_max(false),
        tx_index_tgraph_aggr_duration(false),
        tx_index_tgraph_temporal_condition(false),
        tx_import_static_data(false),
        tx_import_temporal_data(false),
        tx_query_reachable_area(true),
        tx_query_node_neighbor_road(true),
        tx_query_road_earliest_arrive_time_aggr(true),
        tx_query_snapshot(true),
        tx_query_snapshot_aggr_max(true),
        tx_query_snapshot_aggr_duration(true),
        tx_query_road_by_temporal_condition(true);
        private boolean isReadTx;
        TxType(boolean isReadTx){
            this.isReadTx = isReadTx;
        }
        public boolean isReadTx() {
            return isReadTx;
        }
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
