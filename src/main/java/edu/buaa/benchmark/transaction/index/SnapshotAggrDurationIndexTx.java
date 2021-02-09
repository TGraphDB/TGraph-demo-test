package edu.buaa.benchmark.transaction.index;

import edu.buaa.benchmark.transaction.SnapshotAggrDurationTx;

public class SnapshotAggrDurationIndexTx extends SnapshotAggrDurationTx implements QueryWithIndexTx {
    private long indexId;

    public void setIndexId(long indexId) {
        this.indexId = indexId;
    }

    @Override
    public long getIndexId() {
        return indexId;
    }
}
