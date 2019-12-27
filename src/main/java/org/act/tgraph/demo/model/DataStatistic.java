package org.act.tgraph.demo.model;

import org.act.tgraph.demo.algo.BreadthFirstRelTraversal;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DataStatistic {
    public static void main(String[] args) throws IOException {
        TrafficTemporalPropertyGraph tgraph = new TrafficTemporalPropertyGraph();
        tgraph.importTopology(new File("/tmp/road_topology.csv"));
//        isolatedGraph(tgraph);
//        confirmDualReference(tgraph);
//        System.out.println(tgraph.getRoadEndCross("595662_42964").detail());
//        addNewRouteCount(tgraph);
        tgraph.importTraffic(buildFileList("/tmp/traffic", Arrays.asList("0501.csv"))); // , "0502.csv", "0503.csv", "0504.csv", "0505.csv", "0506.csv", "0507.csv"
    }

    private static List<File> buildFileList(String dir, List<String> files) {
        File folder = new File(dir);
        if(!folder.exists() && !folder.mkdirs()) throw new RuntimeException("can not create dir.");
        return files.stream().map(s -> new File(folder, s)).collect(Collectors.toList());
    }

    /**
     *
     * @param tgraph
     * Result: 120127 roads, 77 groups, with 1 largest group, the others 76 group has totally 90 roads.
     */
    private static void isolatedGraph(TrafficTemporalPropertyGraph tgraph){
        int groupId = 1;
        Set<RoadRel> notVisited = new HashSet<>(tgraph.getAllRoads());
        while(!notVisited.isEmpty()){
            RoadRel randomSelectRoad = notVisited.iterator().next();
            BreadthFirstRelTraversal bfsIter = new BreadthFirstRelTraversal(randomSelectRoad);
            while(bfsIter.hasNext()){
                RoadRel road = bfsIter.next();
                notVisited.remove(road);
                System.out.println(groupId+"      "+road);
            }
            groupId++;
        }
    }

    // Dual reference confirmed, if A refer B as A's outRoad, then B must refer A as B's inRoad.
    private static void confirmDualReference(TrafficTemporalPropertyGraph tgraph){
        Map<String, Integer> edgePairSet = new HashMap<>();
        tgraph.getAllRoads().forEach(road -> {
            road.inChains.forEach(roadRel -> edgePairSet.compute(roadRel.id+", "+road.id, (k,cnt)-> cnt==null ? 1 : cnt+1));
            road.outChains.forEach(roadRel -> edgePairSet.compute(road.id+", "+roadRel.id, (k,cnt)-> cnt==null? 1 : cnt+1));
        });
        edgePairSet.forEach((edgePair, cnt) -> {
            if(cnt!=2) System.out.println(edgePair+", "+cnt);
        });
    }

    /**
     * Count the strange case in {@code CrossNode.fillInOutRoadSetToFull()}
     * @param tgraph
     * !!!Got unexpected result. 181881 original routes, 40671 new routes.
     * Unexpected rule for all new routes: abs(grid id diff) is 30000 and same type, same length, abs(angle diff) is 180
     * new route: 595662_42964, 595662_12964  0  0  180
     * new route: 595673_47320, 595673_17320  0  0  -180
     * new route: 595673_17317, 595673_47317  0  0  180
     * checkout this cross (595662_42964 end cross) for example: [A,B]|[C,D,E]
     * (B),LEN9,TYPE4,ANGLE188,IN[595662_12969],OUT[E,C]
     * (A),LEN140,TYPE11,ANGLE8,IN[595662_13193,595662_12950],OUT[E,C,D]
     * (C),LEN62,TYPE2,ANGLE348,IN[B,A],OUT[595662_08188]
     * (D),LEN9,TYPE4,ANGLE8,IN[A],OUT[595662_12965]
     * (E),LEN177,TYPE6,ANGLE88,IN[B,A],OUT[595662_12970,595662_12966]
     *
     * there are 98 route in original case match above rule (except abs(grid id diff) is 30000)
     */
    private static void addNewRouteCount(TrafficTemporalPropertyGraph tgraph){
        Set<String> edgePairSet = new HashSet<>();
        tgraph.getAllRoads().forEach(road -> {
            road.inChains.forEach(roadRel -> edgePairSet.add(roadRel.id+","+road.id));
            road.outChains.forEach(roadRel -> edgePairSet.add(road.id+","+roadRel.id));
        });
        System.out.println(edgePairSet.size()+" original routes.");
        AtomicInteger cnt = new AtomicInteger();
        tgraph.getAllCross().forEach(crossNode -> {
            crossNode.inRoads.forEach(inRoad -> {
                crossNode.outRoads.forEach(outRoad -> {
                    String pair = inRoad.id + "," + outRoad.id;
                    if(!edgePairSet.contains(pair)){
                        System.out.println("new route: "+ pair+"  "+(inRoad.getType()-outRoad.getType())+"  "+(inRoad.length-outRoad.length)+"  "+(inRoad.angle-outRoad.angle));
//                        System.out.println("new route: "+ pair+"  "+inRoad.getType()+"  "+inRoad.length+"  "+outRoad.angle);
                        cnt.getAndIncrement();
                    }
                });
            });
        });
        System.out.println(cnt.get()+" new routes.");
        //if other routes match that rule
        cnt.set(0);
        edgePairSet.forEach(pair -> {
            String[] arr = pair.split(",");
            RoadRel inRoad = tgraph.getRoadRel(arr[0]);
            RoadRel outRoad = tgraph.getRoadRel(arr[1]);
            if(inRoad.getType()==outRoad.getType() && inRoad.length==outRoad.length && Math.abs(inRoad.angle-outRoad.angle)==0){
                System.out.println(pair+"  "+inRoad.getType()+"  "+inRoad.length+"  "+outRoad.angle);
                cnt.getAndIncrement();
            }
        });
        System.out.println(cnt.get()+" case in original routes match that strange rule.");
    }

}
