package org.act.tgraph.demo.benchmark.client;

import org.act.tgraph.demo.benchmark.DBOperation;
import org.act.tgraph.demo.benchmark.TransactionExecutor;
import org.act.tgraph.demo.benchmark.transaction.ImportStaticDataTx;
import org.act.tgraph.demo.benchmark.transaction.ImportTemporalDataTx;
import org.act.tgraph.demo.benchmark.transaction.ReachableAreaQueryTx;
import org.act.tgraph.demo.client.TGraphTcpConnection;
import org.act.tgraph.demo.utils.LoggerProxy;
import org.act.tgraph.demo.utils.TimeMonitor;

import java.io.IOException;

public class TGraphExecutorClient implements TransactionExecutor, DBOperation {
    private final TGraphTcpConnection conn;

    TGraphExecutorClient(String host, int port) throws IOException {
        this.conn = new TGraphTcpConnection(host, port);
    }

    @Override
    public void createDB() throws IOException {}

    @Override
    public void restartDB() throws IOException {

    }

    @Override
    public void shutdownDB() throws IOException {

    }

    @Override
    public void execute(ImportStaticDataTx tx) throws IOException {
        TimeMonitor tMonitor = new TimeMonitor();
        String req = tx.encodeArgs().toString();
        String result = conn.call(req, tMonitor);

    }

    @Override
    public void execute(ImportTemporalDataTx tx) throws IOException {
        TimeMonitor tMonitor = new TimeMonitor();
        String req = tx.encodeArgs().toString();
        String result = conn.call(req, tMonitor);
    }

    @Override
    public void execute(ReachableAreaQueryTx tx) throws IOException {
        TimeMonitor tMonitor = new TimeMonitor();
        String req = tx.encodeArgs().toString();
        String result = conn.call(req, tMonitor);
    }
}
