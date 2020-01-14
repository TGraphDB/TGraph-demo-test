package org.act.tgraph.demo.benchmark.transaction;

import org.act.tgraph.demo.benchmark.ResultChecker;
import org.act.tgraph.demo.client.driver.DBOperationProxy;
import org.act.tgraph.demo.utils.LoggerProxy;

public class NodeDegreeCountTx implements AbstractTransaction {


    @Override
    public boolean execute(LoggerProxy log, DBOperationProxy db, ResultChecker checker) {

        return false;
    }

    @Override
    public String encode() {
        return null;
    }
}
