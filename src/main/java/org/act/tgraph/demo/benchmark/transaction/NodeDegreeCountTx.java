package org.act.tgraph.demo.benchmark.transaction;

import org.act.tgraph.demo.benchmark.ResultChecker;
import org.act.tgraph.demo.benchmark.DBOperation;
import org.act.tgraph.demo.utils.LoggerProxy;

public class NodeDegreeCountTx implements AbstractTransaction {


    @Override
    public boolean execute(LoggerProxy log, DBOperation db, ResultChecker checker) {

        return false;
    }

    @Override
    public String encodeArgs() {
        return null;
    }
}
