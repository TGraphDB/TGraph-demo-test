package edu.buaa.benchmark;

import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.benchmark.transaction.ImportStaticDataTx;
import edu.buaa.benchmark.transaction.ImportTemporalDataTx;
import edu.buaa.benchmark.transaction.ReachableAreaQueryTx;

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
