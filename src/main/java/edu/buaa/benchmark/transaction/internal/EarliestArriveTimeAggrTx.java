package edu.buaa.benchmark.transaction.internal;

import com.google.common.base.Preconditions;
import edu.buaa.benchmark.transaction.AbstractTransaction;

public class EarliestArriveTimeAggrTx extends AbstractTransaction {
    private long roadId;
    private int departureTime;
    private int endTime;

    public EarliestArriveTimeAggrTx(long roadId, int departureTime, int endTime, int arriveTime, int updateCnt){
        this.roadId = roadId;
        this.departureTime = departureTime;
        this.endTime = endTime;
        Result r = new Result();
        r.setArriveTime(arriveTime);
        this.setResult(r);
        this.setTxType(TxType.tx_query_road_earliest_arrive_time_aggr);
        Metrics m = new Metrics();
        m.setReqSize(updateCnt);
        this.setMetrics(m);
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
        // if it unable to calculate the time, then this should be set to -1
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
