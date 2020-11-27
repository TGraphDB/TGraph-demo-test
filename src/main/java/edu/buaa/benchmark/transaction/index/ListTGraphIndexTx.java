package edu.buaa.benchmark.transaction.index;

import edu.buaa.benchmark.transaction.AbstractTransaction;

import java.util.List;

public class ListTGraphIndexTx extends AbstractTransaction {

    public static class Result extends AbstractTransaction.Result{
        List<String> indexes;

        public List<String> getIndexes() {
            return indexes;
        }

        public void setIndexes(List<String> indexes) {
            this.indexes = indexes;
        }
    }
}
