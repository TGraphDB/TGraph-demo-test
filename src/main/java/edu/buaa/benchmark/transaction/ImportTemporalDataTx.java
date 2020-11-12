package edu.buaa.benchmark.transaction;

import edu.buaa.model.StatusUpdate;

import java.util.List;

public class ImportTemporalDataTx extends AbstractTransaction {
    public transient int bucket;
    public List<StatusUpdate> data;
    public ImportTemporalDataTx(){} // default constructor and getter setter are needed by json encode/decode.
    public ImportTemporalDataTx(List<StatusUpdate> lines) {
        this.setTxType(TxType.tx_import_temporal_data);
        this.data = lines;
        Metrics m = new Metrics();
        m.setReqSize(lines.size());
        this.setMetrics(m);
    }

    public ImportTemporalDataTx(List<StatusUpdate> lines, int bucket) {
        this.bucket = bucket;
        this.setTxType(TxType.tx_import_temporal_data);
        this.data = lines;
        Metrics m = new Metrics();
        m.setReqSize(lines.size());
        this.setMetrics(m);
    }

    public List<StatusUpdate> getData() {
        return data;
    }

    public void setData(List<StatusUpdate> data) {
        this.data = data;
    }

}
