package edu.buaa.benchmark.transaction;

import edu.buaa.model.StatusUpdate;

import java.util.List;

public class ImportTemporalDataTx extends AbstractTransaction {
    public List<StatusUpdate> data;
    public ImportTemporalDataTx(List<StatusUpdate> lines) {
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
