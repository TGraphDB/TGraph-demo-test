package org.act.tgraph.demo.benchmark.transaction;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class ImportStaticDataTx extends AbstractTransaction {
    private List<Pair<Long, String>> crosses;
    private List<StaticRoadRel> roads;

    // this constructor is used when generating benchmarks.
    public ImportStaticDataTx(List<Pair<Long, String>> crosses, List<StaticRoadRel> roads) {
        this.setTxType(TxType.tx_import_static_data);
        this.crosses = crosses;
        this.roads = roads;
        Metrics m = new Metrics();
        m.setReqSize(crosses.size()+roads.size());
        this.setMetrics(m);
    }

    public ImportStaticDataTx(){}

    public List<Pair<Long, String>> getCrosses() {
        return crosses;
    }

    public void setCrosses(List<Pair<Long, String>> crosses) {
        this.crosses = crosses;
    }

    public List<StaticRoadRel> getRoads() {
        return roads;
    }

    public void setRoads(List<StaticRoadRel> roads) {
        this.roads = roads;
    }

    public static class StaticRoadRel{
        private long roadId, startCrossId, endCrossId;
        private String id;
        private int length, angle, type;
        public StaticRoadRel(long roadId, long startCrossId, long endCrossId, String id, int length, int angle, int type) {
            this.roadId = roadId;
            this.startCrossId = startCrossId;
            this.endCrossId = endCrossId;
            this.id = id;
            this.length = length;
            this.angle = angle;
            this.type = type;
        }

        public long getRoadId() {
            return roadId;
        }

        public void setRoadId(long roadId) {
            this.roadId = roadId;
        }

        public long getStartCrossId() {
            return startCrossId;
        }

        public void setStartCrossId(long startCrossId) {
            this.startCrossId = startCrossId;
        }

        public long getEndCrossId() {
            return endCrossId;
        }

        public void setEndCrossId(long endCrossId) {
            this.endCrossId = endCrossId;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public int getAngle() {
            return angle;
        }

        public void setAngle(int angle) {
            this.angle = angle;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }
    }
}
