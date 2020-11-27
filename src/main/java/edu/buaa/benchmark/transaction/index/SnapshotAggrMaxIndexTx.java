package edu.buaa.benchmark.transaction.index;

import edu.buaa.benchmark.transaction.SnapshotAggrMaxTx;

public class SnapshotAggrMaxIndexTx extends SnapshotAggrMaxTx implements QueryWithIndexTx {
    private long indexId;

    public void setIndexId(long indexId) {
        this.indexId = indexId;
    }

    @Override
    public long getIndexId() {
        return indexId;
    }
}
