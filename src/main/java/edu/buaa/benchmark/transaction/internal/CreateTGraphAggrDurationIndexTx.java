package edu.buaa.benchmark.transaction.internal;

import edu.buaa.benchmark.transaction.AbstractTransaction;

public class CreateTGraphAggrDurationIndexTx extends AbstractTransaction {
    private int start, end;
    private String proName;
    private int every, timeUnit;

    public CreateTGraphAggrDurationIndexTx(){
        setTxType(TxType.tx_index_tgraph_aggr_duration);
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getProName() {
        return proName;
    }

    public void setProName(String proName) {
        this.proName = proName;
    }

    public int getEvery() {
        return every;
    }

    public void setEvery(int every) {
        this.every = every;
    }

    public int getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(int timeUnit) {
        this.timeUnit = timeUnit;
    }

    public static class Result extends AbstractTransaction.Result{
        private long indexId;

        public long getIndexId() {
            return indexId;
        }

        public void setIndexId(long indexId) {
            this.indexId = indexId;
        }
    }
}
