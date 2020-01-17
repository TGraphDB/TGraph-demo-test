package org.act.tgraph.demo.benchmark.transaction;

import org.act.tgraph.demo.benchmark.ResultChecker;
import org.act.tgraph.demo.benchmark.DBOperation;
import org.act.tgraph.demo.utils.LoggerProxy;

public class NodeDegreeCountTx extends AbstractTransaction {

    protected NodeDegreeCountTx(TxType txType) {
        super(txType);
    }

    @Override
    public String encode() {
        return null;
    }
}
