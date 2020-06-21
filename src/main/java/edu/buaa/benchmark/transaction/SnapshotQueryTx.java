package edu.buaa.benchmark.transaction;

import edu.buaa.algo.EarliestArriveTime;

import java.util.List;

public class SnapshotQueryTx extends AbstractTransaction {

    private int timestamp;

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
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
