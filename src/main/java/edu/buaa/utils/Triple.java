package edu.buaa.utils;

import org.apache.commons.lang.ObjectUtils;

import java.io.Serializable;

public class Triple<L, M, R> implements Serializable {

    private L left;
    private M middle;
    private R right;
    public Triple() {}

    public static <L, M, R> Triple<L, M, R> of(L left, M middle, R right) {
        Triple<L,M,R> t = new Triple<L, M, R>();
        t.left = left;
        t.middle = middle;
        t.right = right;
        return t;
    }

    public L getLeft() { return left; }

    public void setLeft(L left) { this.left = left; }

    public M getMiddle() { return middle; }

    public void setMiddle(M middle) { this.middle = middle; }

    public R getRight() { return right; }

    public void setRight(R right) {this.right = right;}

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Triple)) {
            return false;
        } else {
            Triple<?, ?, ?> other = (Triple)obj;
            return this.getLeft().equals(other.getLeft()) && this.getMiddle().equals(other.getMiddle()) && this.getRight().equals(other.getRight());
        }
    }

    public int hashCode() {
        return (this.getLeft() == null ? 0 : this.getLeft().hashCode()) ^ (this.getMiddle() == null ? 0 : this.getMiddle().hashCode()) ^ (this.getRight() == null ? 0 : this.getRight().hashCode());
    }

    public String toString() {
        return "" + '(' + this.getLeft() + ',' + this.getMiddle() + ',' + this.getRight() + ')';
    }

    public String toString(String format) {
        return String.format(format, this.getLeft(), this.getMiddle(), this.getRight());
    }
}
