package org.act.tgraph.demo.benchmark;

import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;
import org.act.tgraph.demo.benchmark.transaction.ImportStaticDataTx;
import org.act.tgraph.demo.benchmark.transaction.ImportTemporalDataTx;
import org.act.tgraph.demo.benchmark.transaction.ReachableAreaQueryTx;

import java.io.IOException;

public abstract class AbstractTransactionExecutor {

    public AbstractTransaction execute(AbstractTransaction tx) throws IOException {
        if(tx instanceof ImportTemporalDataTx){
            execute((ImportTemporalDataTx) tx);
        }else if(tx instanceof ReachableAreaQueryTx){
            execute((ReachableAreaQueryTx) tx);
        }else if(tx instanceof ImportStaticDataTx){
            execute((ImportStaticDataTx) tx);
        }
        return tx;
    }

    public abstract void execute(ImportStaticDataTx tx) throws IOException;

    public abstract void execute(ImportTemporalDataTx tx) throws IOException;

    public abstract void execute(ReachableAreaQueryTx tx) throws IOException;

}
