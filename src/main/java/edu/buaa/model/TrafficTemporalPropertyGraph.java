package org.act.tgraph.demo.model;

import org.act.tgraph.demo.utils.Helper;
import org.act.tgraph.demo.utils.MultiFileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrafficTemporalPropertyGraph {
    private boolean isTopoSet = false;
    private int timeMin = Integer.MAX_VALUE;
    private int timeMax = -1;

    private Map<String, RoadRel> roadRelMap = new HashMap<>();
    private Map<RoadRel, CrossNode> roadStartCrossNodeMap = new HashMap<>();
    private Map<RoadRel, CrossNode> roadEndCrossNodeMap = new HashMap<>();

    public void importTopology(File roadTopology) throws IOException {
        assert !isTopoSet: "already set topology!";
        if(!roadTopology.exists()) {
            Helper.download("http://amitabha.water-crystal.org/TGraphDemo/Topo.csv.gz", roadTopology);
        }
        try (BufferedReader br = Helper.gzipReader(roadTopology)) {
            br.readLine();// skip first line;
            String line;
            while ((line = br.readLine()) != null) {
                RoadRel.createOrUpdate(roadRelMap, line);
            }
        }
        isTopoSet = true;
        buildCrossFromRoad();
    }

    public void importTraffic(List<File> trafficData) {
        try(MultiFileReader iterator = new MultiFileReader(trafficData)) {
            while (iterator.hasNext()) {
                StatusUpdate s = iterator.next();
                TimePointInt tStart = new TimePointInt(s.time);
                RoadRel road = roadRelMap.get(s.roadId);
                road.tpJamStatus.setToNow(tStart, s.jamStatus);
                road.tpSegCount.setToNow(tStart, s.segmentCount);
                road.tpTravelTime.setToNow(tStart, s.travelTime);
                Integer cnt = road.updateCount.get(TimePointInt.Now);
                if(cnt==null) road.updateCount.setToNow(tStart, 1);
                else road.updateCount.setToNow(tStart, cnt+1);
                if (timeMax < s.time) timeMax = s.time;
                if (timeMin > s.time) timeMin = s.time;
            }
        }
    }

    public RoadRel getRoadRel(String roadId){
        return roadRelMap.get(roadId);
    }

    private void buildCrossFromRoad() {
        for(RoadRel road : getAllRoads()){
            if(roadEndCrossNodeMap.get(road)==null){
                buildCrossFromRoad(Collections.singleton(road), road.outChains);
            }
            if(roadStartCrossNodeMap.get(road)==null){
                buildCrossFromRoad(road.inChains, Collections.singleton(road));
            }
        }
    }

    private void buildCrossFromRoad(Collection<RoadRel> inRoads, Collection<RoadRel> outRoads) {
        Set<RoadRel> in = new HashSet<>(inRoads);
        Set<RoadRel> out = new HashSet<>(outRoads);
        CrossNode.fillInOutRoadSetToFull(in, out);
        CrossNode cross = new CrossNode(in, out);
        in.forEach(roadRel -> roadEndCrossNodeMap.put(roadRel, cross));
        out.forEach(roadRel -> roadStartCrossNodeMap.put(roadRel, cross));
    }

    public static void main(String[] args) throws IOException {
        Map<String, Integer> edgePairSet = new HashMap<>();
        TrafficTemporalPropertyGraph tgraph = new TrafficTemporalPropertyGraph();
        tgraph.importTopology(new File("/tmp/road_topology.csv.gz"));
        tgraph.roadRelMap.forEach((id, road) -> {
            road.inChains.forEach(roadRel -> edgePairSet.compute(roadRel.id + ", " + road.id, (s,cnt)-> cnt==null ? 1 : cnt+1));
            road.outChains.forEach(roadRel -> edgePairSet.compute(road.id+", "+roadRel.id, (s,cnt)-> cnt==null? 1: cnt+1));
        });
        edgePairSet.forEach((edgePair, cnt) -> {
            if(cnt!=2) System.out.println(edgePair+", "+cnt);
        });
//        tgraph.importTraffic(Collections.singletonList(new File("/tmp/0501.csv")));
    }

    public Collection<RoadRel> getAllRoads() {
        return roadRelMap.values();
    }

    public Collection<CrossNode> getAllCross() {
        Set<CrossNode> result = new HashSet<>();
        result.addAll(roadStartCrossNodeMap.values());
        result.addAll(roadEndCrossNodeMap.values());
        return result;
    }

    public CrossNode getRoadStartCross(RoadRel road) {
        return roadStartCrossNodeMap.get(road);
    }

    public CrossNode getRoadEndCross(RoadRel road) {
        return roadEndCrossNodeMap.get(road);
    }

    public long compress() {
        long totalRmCnt = 0;
        for(RoadRel road : getAllRoads()){
            totalRmCnt += road.tpTravelTime.mergeSameVal();
            totalRmCnt += road.tpSegCount.mergeSameVal();
            totalRmCnt += road.tpJamStatus.mergeSameVal();
        }
        return totalRmCnt;
    }


    public int getTimeMin() {
        return timeMin;
    }

    public int getTimeMax() {
        return timeMax;
    }
}
