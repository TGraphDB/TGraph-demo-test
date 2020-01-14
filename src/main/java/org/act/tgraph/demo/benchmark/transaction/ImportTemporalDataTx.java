package org.act.tgraph.demo.benchmark.transaction;

import org.act.tgraph.demo.benchmark.ResultChecker;
import org.act.tgraph.demo.client.driver.DBOperationProxy;
import org.act.tgraph.demo.utils.LoggerProxy;

import java.util.List;

public class ImportTemporalDataTx implements AbstractTransaction {
    private StatusUpdate[] data;
    public ImportTemporalDataTx(List<StatusUpdate> lines) {
        this.data = lines.toArray(new StatusUpdate[0]);
    }


    @Override
    public boolean execute(LoggerProxy log, DBOperationProxy db, ResultChecker checker) {
        DBOperationProxy.TxProxy tx = db.beginTx();
        for(StatusUpdate s : data){
            tx.setTemporalProp(s.roadId,s.time,s.jamStatus,s.segmentCount,s.travelTime);
        }
        tx.commit();
        return false;
    }

    @Override
    public String encode() {
        return null;
    }

    public static class StatusUpdate{
        public final long roadId;
        public final int time;
        public final int travelTime;
        public final int jamStatus;
        public final int segmentCount;

        public StatusUpdate(long roadId, int time, int travelTime, int jamStatus, int segmentCount) {
            this.roadId = roadId;
            this.time = time;
            this.travelTime = travelTime;
            this.jamStatus = jamStatus;
            this.segmentCount = segmentCount;
        }
    }
}
