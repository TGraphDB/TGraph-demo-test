package edu.buaa.benchmark.transaction;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class SnapshotAggrMaxTx extends AbstractTransaction {
    private int t0;
    private int t1;
    private String p;

    public int getT0() {
        return t0;
    }

    public int getT1() {
        return t1;
    }

    public String getP() {
        return p;
    }

    public void setT0(int t0) {
        this.t0 = t0;
    }

    public void setT1(int t1) {
        this.t1 = t1;
    }

    public void setP(String p) {
        this.p = p;
    }

    public static class Result extends AbstractTransaction.Result{
        List<Pair<Long, Integer>> roadTravelTime;

        public List<Pair<Long, Integer>> getRoadTravelTime() {
            return roadTravelTime;
        }

        public void setRoadTravelTime(List<Pair<Long, Integer>> roadTravelTime) {
            this.roadTravelTime = roadTravelTime;
        }
    }
}
