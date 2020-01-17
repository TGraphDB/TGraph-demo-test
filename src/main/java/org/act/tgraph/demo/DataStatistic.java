package org.act.tgraph.demo;

import org.act.tgraph.demo.algo.BreadthFirstRelTraversal;
import org.act.tgraph.demo.model.RoadRel;
import org.act.tgraph.demo.model.TimePointInt;
import org.act.tgraph.demo.model.TrafficTemporalPropertyGraph;
import org.act.tgraph.demo.utils.Helper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DataStatistic {
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static void main(String[] args) throws IOException {
        TrafficTemporalPropertyGraph tgraph = new TrafficTemporalPropertyGraph();
        long start = System.currentTimeMillis();
        tgraph.importTopology(new File("/tmp/road_topology.csv.gz"));
//        isolatedGraph(tgraph);
//        confirmDualReference(tgraph);
//        System.out.println(tgraph.getRoadEndCross("595662_42964").detail());
//        addNewRouteCount(tgraph);
        System.out.println("import topology time: "+ (System.currentTimeMillis()-start));
        start = System.currentTimeMillis();
        tgraph.importTraffic(Helper.trafficFileList("/tmp/traffic", "0503", "0507")); //"0501.csv", "0502.csv",
        System.out.println("import time: "+ (System.currentTimeMillis()-start)/1000);
//        new Scanner(System.in).nextLine();
        start = System.currentTimeMillis();
//        long rmCnt = tgraph.compress();
//        System.out.println("compress time: "+ (System.currentTimeMillis()-start));
//        System.out.println("compress remove "+rmCnt+" entries. ");
//        roadUpdateDistribution(tgraph);
//        totalTravelTimeEvolve(tgraph);
        Pair<TimePointInt, Set<RoadRel>> tmp = mostFreqValueUpdateRoads(tgraph, 3600, 24);
        System.out.println("get "+tmp.getRight().size()+" roads.");
//        double lenPercent = roadLengthPercent(tgraph, tmp.getRight());
//        System.out.println("length percent: "+lenPercent);
        double dataPercent = roadDataPercent(tmp.getRight(), tgraph);
        System.out.println("data percent: "+dataPercent);
        System.out.println("calc time: "+ (System.currentTimeMillis()-start));
//        new Scanner(System.in).nextLine();
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

    /**
     *
     * @param tgraph
     * Results show when importing 0501 traffic data (6667610 lines, 190MB, result in 6667610*3=20002830 entries), it use 30 seconds and cost about 2GB memory.
     * after compress (about 1 seconds), 11476236 entries are removed (about 1/2 is redundant).
     */
    private static void sameValCompress(TrafficTemporalPropertyGraph tgraph){
        long start = System.currentTimeMillis();
        long rmCnt = tgraph.compress();
        System.out.println("compress time: "+ (System.currentTimeMillis()-start));
        System.out.println("compress remove "+rmCnt+" entries. ");
    }

    /**
     * distribution of road tp value update.
     * xAxis: roads (order by yAxis, desc)
     * yAxis: value update count
     */
    private static List<Pair<RoadRel, IntSummaryStatistics>> roadUpdateDistribution(TrafficTemporalPropertyGraph tgraph){
        assert  tgraph.compress()==0 : "need compress.";
        List<Pair<RoadRel, IntSummaryStatistics>> results = tgraph.getAllRoads().parallelStream().map(road -> {
            IntSummaryStatistics stat = h_countIntervalLength(road.tpJamStatus.intervalEntries());
            return Pair.of(road, stat);
        }).collect(Collectors.toList());
        System.out.println("result count: "+results.size());
        results.sort((o1, o2) -> (int) (o2.getRight().getCount() - o1.getRight().getCount()));
        for(Pair<RoadRel, IntSummaryStatistics> pair : results){
            System.out.println(pair.getLeft().id+" "+pair.getRight());
        }
        return results;
    }
    private static <E> IntSummaryStatistics h_countIntervalLength(Iterator<Triple<TimePointInt, TimePointInt, E>> tpValIter){
        Iterable<Triple<TimePointInt, TimePointInt, E>> tp = ()-> tpValIter;
        return StreamSupport.stream(tp.spliterator(), false)
                .filter(entry -> !entry.getMiddle().isNow())
                .map(entry -> {
            TimePointInt startT = entry.getLeft();
            TimePointInt endT = entry.getMiddle();
            return endT.val() - startT.val();
        }).collect(Collectors.summarizingInt(Integer::intValue));
    }

    /**
     * temporal property value evolve in a given period of time.
     * must have continues data (no time gap)
     * only calculate a subset of roads (some roads may upload no data in two days! for data value update distribution, see {@code roadUpdateDistribution()})
     * @param tgraph
     *
     */
    private static void totalTravelTimeEvolve(TrafficTemporalPropertyGraph tgraph){
        assert  tgraph.compress()==0 : "need compress.";
//        Pair<TimePointInt, Set<RoadRel>> tmp = continueUpdateRoads(tgraph, 80000, sdf);
        Pair<TimePointInt, Set<RoadRel>> tmp = mostFreqValueUpdateRoads(tgraph, 3600, 24);
        System.out.println("get "+tmp.getRight().size()+" roads.");
        double lenPercent = roadLengthPercent(tgraph, tmp.getRight());
        System.out.println("length percent: "+lenPercent);
        int sampleTimeInterval = 600; //seconds
        for(int t=tmp.getLeft().val(); t<tgraph.getTimeMax(); t+=sampleTimeInterval){
            TimePointInt time = new TimePointInt(t);
            long totalTravelTime = 0;
            int roadCnt = 0;
            for (RoadRel road : tmp.getRight()) {
                Integer travelTime = road.tpTravelTime.get(time);
                if(travelTime != null){
                    totalTravelTime += travelTime;
                    roadCnt++;
                }
            }
            System.out.println(sdf.format(new Date(time.val()*1000L))+" "+(totalTravelTime/roadCnt)+" "+roadCnt);//
        }
    }
    // find the first time at which there are at least `leastRoadCount` roads have the give temporal property (get!=null).
    // and pack those roads in the result set.
    private static Pair<TimePointInt, Set<RoadRel>> continueUpdateRoads(TrafficTemporalPropertyGraph tgraph, int leastRoadCount){
        long l = tgraph.getTimeMin();
        long r = tgraph.getTimeMax();
        long mid = (l+r+1)/2;
        while(l<mid && mid<r){
            TimePointInt time = new TimePointInt(Math.toIntExact(mid));
            long roadCnt = tgraph.getAllRoads().parallelStream().filter(road-> road.tpTravelTime.get(time)!=null).count();
            if(roadCnt>leastRoadCount){
                r = mid;
                mid = (l+r+1)/2;
            }else if(roadCnt<leastRoadCount){
                l = mid;
                mid = (l+r+1)/2;
            }else{
                break;
            }
            System.out.println("mid,cnt: "+sdf.format(new Date(mid*1000))+" "+roadCnt);
        }
        TimePointInt time = new TimePointInt(Math.toIntExact(mid));
        return Pair.of(time, tgraph.getAllRoads().parallelStream().filter(road-> road.tpTravelTime.get(time)!=null).collect(Collectors.toSet()));
    }
    // find roads whose largest value update period less than `largestUpdatePeriod` (in seconds)
    // and find the first time at which all of the above roads' temporal property have values.
    private static Pair<TimePointInt, Set<RoadRel>> mostFreqValueUpdateRoads(TrafficTemporalPropertyGraph tgraph, int largestUpdatePeriod, int updateCnt){
        List<Pair<RoadRel, IntSummaryStatistics>> candidate = roadUpdateDistribution(tgraph);
        Set<RoadRel> result = candidate.stream()
                .filter(pair -> {
                    IntSummaryStatistics s = pair.getRight();
                    return s.getMax()<=largestUpdatePeriod && s.getCount()>= updateCnt;
                }).map(Pair::getLeft).collect(Collectors.toSet());
        long l = tgraph.getTimeMin();
        long r = tgraph.getTimeMax();
        long mid = (l+r+1)/2;
        while(l<mid && mid<r){
            TimePointInt time = new TimePointInt(Math.toIntExact(mid));
            long roadCnt = result.parallelStream().filter(road-> road.tpTravelTime.get(time)!=null).count();
            if(roadCnt>=result.size()){
                r = mid;
                mid = (l+r+1)/2;
            }else {
                l = mid;
                mid = (l+r+1)/2;
            }
            System.out.println("mid,cnt: "+sdf.format(new Date(mid*1000))+" "+roadCnt);
        }
        TimePointInt time = new TimePointInt(Math.toIntExact(mid));
        return Pair.of(time, result);
    }
    private static double roadLengthPercent(TrafficTemporalPropertyGraph tgraph, Collection<RoadRel> roads){
        Optional<Integer> subsetLen = roads.stream().map(roadRel -> roadRel.length).reduce(Integer::sum);
        Optional<Integer> totalLen = tgraph.getAllRoads().stream().map(roadRel -> roadRel.length).reduce(Integer::sum);
        if(subsetLen.isPresent() && totalLen.isPresent()){
            System.out.println("total length: "+totalLen.get()+" subset length: "+subsetLen.get());
            return subsetLen.get()*1.0d/totalLen.get();
        }else{
            return -1;
        }
    }

    private static double roadDataPercent(Set<RoadRel> roads, TrafficTemporalPropertyGraph tgraph){
        Optional<Integer> totalUpdateCnt = tgraph.getAllRoads().parallelStream().filter(roadRel -> roadRel.updateCount.latestTime()!=null).map(roadRel -> roadRel.updateCount.get(TimePointInt.Now)).reduce(Integer::sum);
        Optional<Integer> roadsUpdateCnt = tgraph.getAllRoads().parallelStream().filter(roads::contains).map(roadRel -> roadRel.updateCount.get(TimePointInt.Now)).reduce(Integer::sum);
        if(totalUpdateCnt.isPresent() && roadsUpdateCnt.isPresent()){
            System.out.println("total update cnt: "+totalUpdateCnt.get()+" subset update cnt: "+roadsUpdateCnt.get());
            return roadsUpdateCnt.get()*1.0d/totalUpdateCnt.get();
        }else{
            return -1;
        }
    }

}
