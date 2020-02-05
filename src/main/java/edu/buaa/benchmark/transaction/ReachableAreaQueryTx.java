package edu.buaa.benchmark.transaction;

import com.google.common.base.Preconditions;
import edu.buaa.algo.EarliestArriveTime;
import edu.buaa.algo.EarliestArriveTime.NodeCross;

import java.util.*;

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
        List<NodeCross> expected = ((Result) this.getResult()).getNodeArriveTime();
        List<NodeCross> got = ((Result) result).getNodeArriveTime();
        if(got.size()!=expected.size()){
            System.out.println("size not match, got "+got.size()+" expect "+expected.size());
            Set<NodeCross> intersection = new HashSet<>(expected);
            intersection.retainAll(got);
            Set<NodeCross> expDiff = new HashSet<>(expected);
            expDiff.removeAll(intersection);
            if(!expDiff.isEmpty()) expDiff.forEach(System.out::println);
            System.out.println("exp-common ^^ vv got-common");
            Set<NodeCross> gotDiff = new HashSet<>(got);
            gotDiff.removeAll(intersection);
//            if(gotDiff.size()>expDiff.size()){
//                printDiff(gotDiff, expDiff);
//            }else{
//                printDiff(expDiff, gotDiff);
//            }
            if(!gotDiff.isEmpty()) gotDiff.forEach(System.out::println);
            System.out.println("--------------------------------------------");
            return;
        }
        for(int i=0; i<got.size(); i++){
            Preconditions.checkState(got.get(i).equals(expected.get(i)));
        }
    }

    private void printDiff(Set<NodeCross> bigger, Set<NodeCross> smaller){
        Map<Long, NodeCross> b = new HashMap<>();
        for(NodeCross n : bigger ){
            b.put(n.getId(), n);
        }
        for(NodeCross n : smaller){
            NodeCross nn = b.get(n.getId());
            if(nn!=null){
                //System.out.println("");
            }
        }
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
