package edu.buaa.benchmark.transaction;

import edu.buaa.algo.EarliestArriveTime.NodeCross;
import edu.buaa.utils.Helper;

import java.util.List;

public class ReachableAreaQueryTx extends AbstractTransaction {
    private long startCrossId;
    private int departureTime;
    private int travelTime;

    public ReachableAreaQueryTx(){}
    public ReachableAreaQueryTx(long startCrossId, int departureTime, int travelTime){
        this.setTxType(TxType.tx_query_reachable_area);
        this.startCrossId = startCrossId;
        this.departureTime = departureTime;
        this.travelTime = travelTime;
    }

    public long getStartCrossId() {
        return startCrossId;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public int getTravelTime() {
        return travelTime;
    }

    public void setStartCrossId(long startCrossId) {
        this.startCrossId = startCrossId;
    }

    public void setDepartureTime(int departureTime) {
        this.departureTime = departureTime;
    }

    public void setTravelTime(int travelTime) {
        this.travelTime = travelTime;
    }

    @Override
    public void validateResult(AbstractTransaction.Result result){
        Helper.validateResult(((Result) this.getResult()).getNodeArriveTime(), ((Result) result).getNodeArriveTime());
    }

    public static class Result extends AbstractTransaction.Result{
        List<NodeCross> nodeArriveTime;

        public List<NodeCross> getNodeArriveTime() {
            return nodeArriveTime;
        }

        public void setNodeArriveTime(List<NodeCross> nodeArriveTime) {
            this.nodeArriveTime = nodeArriveTime;
        }
    }
}
