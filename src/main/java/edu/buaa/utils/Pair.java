package edu.buaa.utils;

import java.io.Serializable;

public class Pair<L, R> implements Serializable {
    private L key;
    private R value;
    public Pair() {}

    public static <L, R> Pair<L, R> of(L left, R right) {
        Pair<L, R> p = new Pair<>();
        p.setKey(left);
        p.setValue(right);
        return p;
    }

    public L getKey() {
        return key;
    }

    public void setKey(L key) {
        this.key = key;
    }

    public R getValue() {
        return value;
    }

    public void setValue(R value) {
        this.value = value;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Pair)) {
            return false;
        } else {
            Pair<?, ?> other = (Pair)obj;
            return this.getKey().equals(other.getKey()) && this.getValue().equals(other.getValue());
        }
    }

    public int hashCode() {
        return (this.getKey() == null ? 0 : this.getKey().hashCode()) ^ (this.getValue() == null ? 0 : this.getValue().hashCode());
    }

    public String toString() { return "" + '(' + this.key + ',' + this.value + ')'; }

}