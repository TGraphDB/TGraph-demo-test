package edu.buaa.benchmark.transaction;

import com.google.common.base.Preconditions;

import java.util.*;

public class NodeNeighborRoadTx extends AbstractTransaction {

    private long nodeId;

    public NodeNeighborRoadTx(long nodeId, List<Long> roadIds) {
        this.setTxType(TxType.tx_query_node_neighbor_road);
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
            System.out.println("size not match, got "+got.size()+" expect "+expected.size());
            return;
        }
        HashSet<Long> intersection = new HashSet<>(got);
        if(intersection.retainAll(expected) && intersection.size()==expected.size()) {
            // do nothing.
        }else{
            Set<Long> gotS = new HashSet<>(got);
            gotS.removeAll(intersection);
            Set<Long> expS = new HashSet<>(expected);
            expS.removeAll(intersection);
            System.out.println("got only: "+ gotS);
            System.out.println("expected only: "+ expS);
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
