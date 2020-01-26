package edu.buaa.benchmark.transaction;

import com.google.common.base.Preconditions;

public class EarliestArriveTimeAggrTx extends AbstractTransaction {
    private long roadId;
    private int departureTime;
    private int endTime;

    public EarliestArriveTimeAggrTx(long roadId, int departureTime, int endTime, int arriveTime){
        this.roadId = roadId;
        this.departureTime = departureTime;
        this.endTime = endTime;
        Result r = new Result();
        r.setArriveTime(arriveTime);
        this.setResult(r);
        this.setTxType(TxType.tx_query_road_earliest_arrive_time_aggr);
    }
    public EarliestArriveTimeAggrTx(){
        this.setTxType(TxType.tx_query_road_earliest_arrive_time_aggr);
    }

    public long getRoadId() {
        return roadId;
    }

    public void setRoadId(long roadId) {
        this.roadId = roadId;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(int departureTime) {
        this.departureTime = departureTime;
    }

    @Override
    public void validateResult(AbstractTransaction.Result result) {
        Result r = (Result) result;
        Result exp = (Result) this.getResult();
        Preconditions.checkState(r.arriveTime==exp.arriveTime,
                "expect "+exp.arriveTime+" but got "+r.arriveTime+" for road "+roadId+" start from "+departureTime);
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public static class Result extends AbstractTransaction.Result{
        private int arriveTime;

        public Result(int minArriveTime) {
            this.arriveTime = minArriveTime;
        }
        public Result(){}

        public int getArriveTime() {
            return arriveTime;
        }

        public void setArriveTime(int arriveTime) {
            this.arriveTime = arriveTime;
        }
    }
}