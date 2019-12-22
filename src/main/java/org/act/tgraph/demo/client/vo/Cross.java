package org.act.tgraph.demo.client.vo;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by song on 16-1-22.
 */
public class Cross {
//    private static Logger logger = new Config().logger;
    private static int count=0;
    private final static Map<RoadChain,Cross> endCrossOfRoad = new HashMap<>();
    private final static Map<RoadChain,Cross> startCrossOfRoad = new HashMap<>();

    public final String id;
    private long nodeId;
    private boolean hasSetNode=false;

    public static Cross getStartCross(RoadChain roadChain){
        Cross cross = startCrossOfRoad.get(roadChain);
        if(cross!=null) {
            return cross;
        }else{
            Set<RoadChain> inSet = new HashSet<>(); // roads direct into a cross.
            Set<RoadChain> outSet = new HashSet<>(); // roads come out from a cross.

            outSet.add(roadChain);
            inSet.addAll(roadChain.getInChains());

            fillInOutRoadSetToFull(inSet, outSet);
            String id = getCrossId(inSet, outSet);
//            logger.info("cross id:{},{},{}",id,roadChain.briefInChain(),roadChain.briefOutChain());
            cross = new Cross(id);
            count++;
            mapCrossToRoads(cross, inSet, outSet);
            return cross;
        }
    }

    public static Cross getEndCross(RoadChain roadChain){
        Cross cross = endCrossOfRoad.get(roadChain);
        if(cross!=null) {
            return cross;
        }else{
            Set<RoadChain> inSet = new HashSet<>(); // roads direct into a cross.
            Set<RoadChain> outSet = new HashSet<>(); // roads come out from a cross.

            inSet.add(roadChain);
            outSet.addAll(roadChain.getOutChains());

            fillInOutRoadSetToFull(inSet, outSet);
            String id = getCrossId(inSet,outSet);
//            logger.info("cross id:{},{},{}",id,roadChain.briefInChain(),roadChain.briefOutChain());
            cross = new Cross(id);
            count++;
            mapCrossToRoads(cross, inSet, outSet);
            return cross;
        }
//        inSet.retainAll(outSet);// inSet now contains only elements in both sets
//        if(inSet.size()>0){
//            System.out.println("has intersection");
//        }

//        System.out.println("ONE OF OUT CHAINS:"+outRoadChain1);
//        System.out.println("BEFORE:"+roadChainSet);
//        System.out.println("ADD:"+outRoadChain1);

//        int tmp = inOutSet.size();
//
//        if(roadChainSet.size()==tmp){
//            count++;
//        }
////        System.out.println("AFTER:"+roadChainSet);
    }

    /**
     * we have to do this because the data has some 'strange' case:
     * CASE 1:
     * let A,B,C,D,E be roads.
     * we found A->C, A->D, B->C, B->E in source data,
     * but there are NO: A->E or B->D!
     * CASE 2:
     * let A,B,C,D,E be roads.
     * we found A->C, B->C, B->D, E->D in source data,
     * but there are NO: A->D or E->C!
     * So we have to repeat filling until we get AB->CDE in CASE 1,
     * and get ABE->CD in CASE 2.
     * @param inSet roads go into a cross
     * @param outSet roads come from a cross
     */
    private static void fillInOutRoadSetToFull(Set<RoadChain> inSet,Set<RoadChain> outSet){
        int inSize = 0;
        int outSize = 0;
        while (inSet.size() > inSize || outSet.size() > outSize) {
            inSize = inSet.size();
            outSize = outSet.size();
            for (RoadChain road : outSet) {
                inSet.addAll(road.getInChains());
            }
            for (RoadChain road : inSet) {
                outSet.addAll(road.getOutChains());
            }
        }
    }

    /**
     * Note: DO NOT confound (in/out roads of a cross) vs (start/end cross of a road)
     * @param cross ~
     * @param inComeRoadsOfCross roads go into the cross
     * @param outComeRoadsOfCross roads go out from the cross
     */
    private static void mapCrossToRoads(Cross cross,
                                       Set<RoadChain> inComeRoadsOfCross,
                                       Set<RoadChain> outComeRoadsOfCross){
        for (RoadChain road : outComeRoadsOfCross) {
            startCrossOfRoad.put(road, cross);
        }
        for (RoadChain road : inComeRoadsOfCross) {
            endCrossOfRoad.put(road, cross);
        }
    }

    private static String getCrossId(Set<RoadChain> inChainSet, Set<RoadChain> outChainSet){
        List<RoadChain> inChains = new ArrayList<>(inChainSet);
        List<RoadChain> outChains = new ArrayList<>(outChainSet);
        inChains.sort(RoadChain.comparator);
        outChains.sort(RoadChain.comparator);
        String inChainStr = RoadChain.list2String(inChains);
        String outChainStr = RoadChain.list2String(outChains);
        return inChainStr+"|"+outChainStr;
    }

    private Cross(String crossId) {
        this.id = crossId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public void setNode(Node node) {
        hasSetNode=true;
        setNodeId(node.getId());
    }

    public Node getNode(GraphDatabaseService db) {
        if(hasSetNode) {
            return db.getNodeById(getNodeId());
        }else{
            return null;
        }
    }

    public static int getTotalCount() {
        return count;
    }

    public long getNodeId() {
        return nodeId;
    }
}



//    public static String getOutCrossId(RoadChain roadChain){
//        Set<RoadChain> inSet = new HashSet<>();
//        Set<RoadChain> outSet = new HashSet<>();
//        int inSize = 0;
//        int outSize = 0;
//
//
//        while(inSet.size()>inSize || outSet.size()>outSize) {
//            inSize = inSet.size();
//            outSize = outSet.size();
//            for (RoadChain road : outSet) {
//                inSet.addAll(road.getInChains());
//            }
//            for (RoadChain road : inSet) {
//                outSet.addAll(road.getOutChains());
//            }
//        }
//
//        return getCrossId(new ArrayList<RoadChain>(inSet), new ArrayList<RoadChain>(outSet));
////        Set<RoadChain> inOutSet = new HashSet<>();
////        inOutSet.addAll(roadChain.outChains);
////        inOutSet.addAll(roadChain.outChains.get(0).inChains);
////        List<RoadChain> roadChains = new ArrayList<>( inOutSet );
////
////        roadChains.sort(new Comparator<RoadChain>() {
////            @Override
////            public int compare(RoadChain o1, RoadChain o2) {
////                return o1.index > o2.index ? 1 : 0;
////            }
////        });
////
////        String[] roadChainIds = new String[roadChains.size()];
////        for(int i=0;i<roadChains.size();i++){
////            roadChainIds[i] = roadChains.get(i).index+"";
////        }
////        String crossId = String.join(",",roadChainIds);
////        return crossId;
//    }
//
//    public static String getInCrossId(RoadChain roadChain){
//        Cross cross = startCrossOfRoad.get(roadChain);
//        if(cross!=null){
//            Set<RoadChain> inSet = new HashSet<>();
//            Set<RoadChain> outSet = new HashSet<>();
//            int inSize = 0;
//            int outSize = 0;
//            outSet.add(roadChain);
//            inSet.addAll(roadChain.getInChains());
//
//            while(inSet.size()>inSize || outSet.size()>outSize) {
//                inSize = inSet.size();
//                outSize = outSet.size();
//                for (RoadChain road : outSet) {
//                    inSet.addAll(road.getInChains());
//                }
//                for (RoadChain road : inSet) {
//                    outSet.addAll(road.getOutChains());
//                }
//            }
//
//            return getCrossId(new ArrayList<RoadChain>(inSet), new ArrayList<RoadChain>(outSet));
////        return getCrossId(roadChain.getInChains(), roadChain.getInChains().get(0).getOutChains());
////        Set<RoadChain> inOutSet = new HashSet<>();
////        inOutSet.addAll(roadChain.inChains);
////        inOutSet.addAll(roadChain.inChains.get(0).outChains);
////        List<RoadChain> roadChains = new ArrayList<>( inOutSet );
////
////        roadChains.sort(new Comparator<RoadChain>() {
////            @Override
////            public int compare(RoadChain o1, RoadChain o2) {
////                return o1.index > o2.index ? 1 : 0;
////            }
////        });
////
////        String[] roadChainIds = new String[roadChains.size()];
////        for(int i=0;i<roadChains.size();i++){
////            roadChainIds[i] = roadChains.get(i).index+"";
////        }
////        String crossId=String.join(",",roadChainIds);
////        return crossId;
//        }