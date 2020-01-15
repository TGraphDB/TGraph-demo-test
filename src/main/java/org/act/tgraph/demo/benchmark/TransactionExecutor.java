package org.act.tgraph.demo.benchmark;

import org.act.tgraph.demo.benchmark.transaction.ImportStaticDataTx;
import org.act.tgraph.demo.benchmark.transaction.ImportTemporalDataTx;
import org.act.tgraph.demo.benchmark.transaction.ReachableAreaQueryTx;

import java.io.IOException;

public interface TransactionExecutor {

    void execute(ImportStaticDataTx tx) throws IOException;

    void execute(ImportTemporalDataTx tx) throws IOException;

    void execute(ReachableAreaQueryTx tx) throws IOException;

}
