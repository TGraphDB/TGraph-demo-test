package org.act.tgraph.demo.benchmark.transaction;

import com.google.common.base.Preconditions;
import org.act.tgraph.demo.algo.EarliestArriveTime;

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
    public void validateResult(AbstractTransaction.Result result) {
        List<EarliestArriveTime.NodeCross> expected = ((Result) this.getResult()).getNodeArriveTime();
        List<EarliestArriveTime.NodeCross> got = ((Result) result).getNodeArriveTime();
        Preconditions.checkArgument(got.size()==expected.size());
        for(int i=0; i<got.size(); i++){
            Preconditions.checkState(got.get(i).equals(expected.get(i)));
        }
    }

    public static class Result extends AbstractTransaction.Result{
        List<EarliestArriveTime.NodeCross> nodeArriveTime;

        public List<EarliestArriveTime.NodeCross> getNodeArriveTime() {
            return nodeArriveTime;
        }

        public void setNodeArriveTime(List<EarliestArriveTime.NodeCross> nodeArriveTime) {
            this.nodeArriveTime = nodeArriveTime;
        }
    }
}
