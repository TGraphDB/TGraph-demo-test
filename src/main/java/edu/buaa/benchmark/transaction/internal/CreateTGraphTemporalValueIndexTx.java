package edu.buaa.benchmark.transaction.internal;

import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.utils.Triple;

import java.util.List;

public class CreateTGraphTemporalValueIndexTx extends AbstractTransaction {
    private int start, end;
    private List<String> props; //proId

    public CreateTGraphTemporalValueIndexTx(){
        setTxType(TxType.tx_index_tgraph_temporal_condition);
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

    public List<String> getProps() {
        return props;
    }

    public void setProps(List<String> props) {
        this.props = props;
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
