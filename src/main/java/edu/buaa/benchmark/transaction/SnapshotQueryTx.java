package edu.buaa.benchmark.transaction;

import edu.buaa.utils.Helper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class SnapshotQueryTx extends AbstractTransaction {

    private int timestamp;

    private String propertyName;

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public static class Result extends AbstractTransaction.Result{
        List<Pair<String, Integer>> roadStatus;

        public List<Pair<String, Integer>> getRoadStatus() {
            return roadStatus;
        }

        public void setRoadStatus(List<Pair<String, Integer>> roadStatus) {
            this.roadStatus = roadStatus;
        }
    }

    @Override
    public void validateResult(AbstractTransaction.Result result){
        Helper.validateResult(((Result) this.getResult()).getRoadStatus(), ((Result) result).getRoadStatus());
    }
}
