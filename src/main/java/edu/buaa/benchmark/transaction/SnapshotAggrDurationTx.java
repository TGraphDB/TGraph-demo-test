package edu.buaa.benchmark.transaction;

import edu.buaa.utils.Helper;
import edu.buaa.utils.Triple;

import java.util.List;

public class SnapshotAggrDurationTx extends AbstractTransaction {

    public SnapshotAggrDurationTx(){
        this.setTxType(TxType.tx_query_snapshot_aggr_duration);
    }
    private int t0;
    private int t1;
    private String p;

    public int getT0() { return t0; }

    public int getT1() {
        return t1;
    }

    public String getP() {
        return p;
    }

    public void setT0(int t0) { this.t0 = t0; }

    public void setT1(int t1) {
        this.t1 = t1;
    }

    public void setP(String p) {
        this.p = p;
    }

    public static class Result extends AbstractTransaction.Result{
        List<Triple<String, Integer, Integer>> roadStatDuration;

        public List<Triple<String, Integer, Integer>> getRoadStatDuration() {
            return roadStatDuration;
        }

        public void setRoadStatDuration(List<Triple<String, Integer, Integer>> roadStatDuration) {
            this.roadStatDuration = roadStatDuration;
        }
    }

    @Override
    public void validateResult(AbstractTransaction.Result result){
        Helper.validateResult(((Result) this.getResult()).getRoadStatDuration(), ((Result) result).getRoadStatDuration());
    }
}
