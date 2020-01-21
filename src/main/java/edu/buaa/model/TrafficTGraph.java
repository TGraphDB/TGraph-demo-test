package edu.buaa.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TrafficTGraph {
    private int timeMin = Integer.MAX_VALUE;
    private int timeMax = -1;

    public final Map<Long, RelRoad> roads = new HashMap<>();
    public final Map<Long, NodeCross> crosses = new HashMap<>();

    public static class RelRoad{
        public final long id;
        public final String name;
        public final int length, angle, type;
        public final NodeCross start, end;
        public final TemporalValue<JamStatus> tpJamStatus = new TemporalValue<>();
        public final TemporalValue<Byte> tpSegCount = new TemporalValue<>();
        public final TemporalValue<Integer> tpTravelTime = new TemporalValue<>();
        public final TemporalValue<Integer> updateCount = new TemporalValue<>();

        public RelRoad(long id, String name, int length, int angle, int type, NodeCross startCross, NodeCross endCross) {
            this.id = id;
            this.name = name;
            this.length = length;
            this.angle = angle;
            this.type = type;
            this.start = startCross;
            this.end = endCross;
        }
    }

    public static class NodeCross{
        public final long id;
        public final String name;
        public final Set<RelRoad> in = new HashSet<>();
        public final Set<RelRoad> out = new HashSet<>();
        public NodeCross(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public enum JamStatus{
        Jam, Slow, Smooth;
        public static JamStatus valueOf(int i){
            switch (i){
                case 1: return Smooth;
                case 2: return Slow;
                case 3: return Jam;
                default: throw new RuntimeException("got "+i);
            }
        }
    }
}
