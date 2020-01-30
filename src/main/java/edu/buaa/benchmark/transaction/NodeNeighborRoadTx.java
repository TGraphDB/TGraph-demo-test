package edu.buaa.benchmark.transaction;

import java.util.List;

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
