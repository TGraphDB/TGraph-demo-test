package edu.buaa.benchmark.transaction.internal;

import edu.buaa.benchmark.transaction.AbstractTransaction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NodeNeighborRoadTx extends AbstractTransaction {

    private long nodeId;

    public NodeNeighborRoadTx(long nodeId, List<Long> roadIds) {
        this.setTxType(TxType.tx_query_node_neighbor_road);
        this.nodeId = nodeId;
        Result r = new Result();
        r.setRoadIds(roadIds);
        this.setResult(r);
    }

    public NodeNeighborRoadTx(){}

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public void validateResult(AbstractTransaction.Result result) {
        List<Long> got = ((Result) result).getRoadIds();
        List<Long> expected = ((Result) this.getResult()).getRoadIds();
        if(got.size()!=expected.size()){
            System.out.println("size not match, got "+got+" expect "+expected+" for node "+nodeId);
        }else{
            if(!got.isEmpty()){
                HashSet<Long> intersection = new HashSet<>(got);
                intersection.retainAll(expected);
                Set<Long> gotS = new HashSet<>(got);
                gotS.removeAll(intersection);
                Set<Long> expS = new HashSet<>(expected);
                expS.removeAll(intersection);
                if (!gotS.isEmpty() || !expS.isEmpty()) {
                    System.out.println("result not match, got "+got+" expect "+expected+" for node "+nodeId);
                }
            }
        }

    }

    public static class Result extends AbstractTransaction.Result{
        List<Long> roadIds;

        public List<Long> getRoadIds() {
            return roadIds;
        }

        public void setRoadIds(List<Long> roadIds) {
            this.roadIds = roadIds;
        }
    }
}
