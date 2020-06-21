package edu.buaa.benchmark.transaction;

public class EntityTemporalConditionTx extends AbstractTransaction {

    private int t0;
    private int t1;
    private String p;
    private int vmin;
    private int vmax;

    public int getT0() {
        return t0;
    }

    public int getT1() {
        return t1;
    }

    public String getP() {
        return p;
    }

    public int getVmin() {
        return vmin;
    }

    public int getVmax() {
        return vmax;
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

    public void setVmin(int vmin) {
        this.vmin = vmin;
    }

    public void setVmax(int vmax) {
        this.vmax = vmax;
    }
}
