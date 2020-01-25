package edu.buaa.benchmark.transaction;

import com.google.common.base.Preconditions;
import edu.buaa.algo.EarliestArriveTime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        if(got.size()!=expected.size()){
            System.out.println("size not match, got "+got.size()+" expect "+expected.size());
            Set<EarliestArriveTime.NodeCross> intersection = new HashSet<>(expected);
            intersection.retainAll(got);
            Set<EarliestArriveTime.NodeCross> expDiff = new HashSet<>(expected);
            expDiff.retainAll(intersection);
            if(!expDiff.isEmpty()) expDiff.forEach(System.out::println);
            System.out.println("exp-common ^^ vv got-common");
            Set<EarliestArriveTime.NodeCross> gotDiff = new HashSet<>(got);
            gotDiff.retainAll(intersection);
            if(!gotDiff.isEmpty()) gotDiff.forEach(System.out::println);
            return;
        }
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
