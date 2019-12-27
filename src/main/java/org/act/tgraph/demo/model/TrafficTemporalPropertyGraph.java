package org.act.tgraph.demo.model;

import org.act.temporalProperty.query.TimeInterval;
import org.act.temporalProperty.query.TimePointL;
import org.act.tgraph.demo.utils.DataDownloader;
import org.act.tgraph.demo.utils.MultiFileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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
            File compressedFile = new File(roadTopology.getParent(), "Topo.csv.gz");
            DataDownloader.download("http://amitabha.water-crystal.org/TGraphDemo/Topo.csv.gz", compressedFile);
            DataDownloader.decompressGZip(compressedFile, roadTopology);
        }
        try (BufferedReader br = new BufferedReader(new FileReader(roadTopology))) {
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
                TimeInterval tInt = new TimeInterval(new TimePointL(s.time));//to NOW
                RoadRel road = roadRelMap.get(s.roadId);
                road.tpJamStatus.put(tInt, s.jamStatus);
                road.tpSegCount.put(tInt, s.segmentCount);
                road.tpTravelTime.put(tInt, s.travelTime);
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
        tgraph.importTopology(new File("/tmp/road_topology.csv"));
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

    public CrossNode getRoadEndCross(String roadId) {
        return roadEndCrossNodeMap.get(getRoadRel(roadId));
    }
}
