package edu.buaa.benchmark;

import edu.buaa.algo.EarliestArriveTime;
import edu.buaa.benchmark.transaction.*;
import edu.buaa.model.StatusUpdate;
import edu.buaa.model.TimePointInt;
import edu.buaa.model.TrafficTGraph;
import edu.buaa.model.TrafficTGraph.JamStatus;
import edu.buaa.model.TrafficTGraph.NodeCross;
import edu.buaa.model.TrafficTGraph.RelRoad;
import edu.buaa.utils.Helper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BenchmarkTxResultGenerator {

    public static void main(String[] args){
        boolean replace = Boolean.parseBoolean(Helper.mustEnv("REACHABLE_AREA_REPLACE_TX"));
        String benchmarkInputFileName = Helper.mustEnv("BENCHMARK_FILE_INPUT");
        String benchmarkOutputFileName = Helper.mustEnv("BENCHMARK_FILE_OUTPUT");

        try {
            BenchmarkTxResultGenerator resultGen = new BenchmarkTxResultGenerator(replace);
            BenchmarkReader reader = new BenchmarkReader(new File(benchmarkInputFileName));
            BenchmarkWriter writer = new BenchmarkWriter(new File(benchmarkOutputFileName));
            while (reader.hasNext()) {
                writer.write(resultGen.execute(reader.next()));
            }
            reader.close();
            writer.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private final boolean replaceReachableAreaTx;
    private TrafficTGraph tgraph = new TrafficTGraph();

    public BenchmarkTxResultGenerator(boolean replace) {
        this.replaceReachableAreaTx = replace;
    }

    public List<AbstractTransaction> execute(AbstractTransaction tx) throws IOException {
        switch (tx.getTxType()){
            case tx_import_static_data: execute((ImportStaticDataTx) tx);
                break;
            case tx_import_temporal_data: execute((ImportTemporalDataTx) tx);
                break;
            case tx_query_reachable_area: execute((ReachableAreaQueryTx) tx);
                break;
            case tx_query_snapshot: execute((SnapshotQueryTx) tx);
                break;
        }
        return Collections.singletonList(tx);
    }

    public void execute(ImportStaticDataTx tx){
        for(ImportStaticDataTx.StaticCrossNode node : tx.getCrosses()){
            NodeCross n = new NodeCross(node.getId(), node.getName());
            tgraph.crosses.put(node.getId(), n);
        }
        for(ImportStaticDataTx.StaticRoadRel road : tx.getRoads()){
            RelRoad r = new RelRoad(road.getRoadId(), road.getId(), road.getLength(), road.getAngle(), road.getType(),
                    tgraph.crosses.get(road.getStartCrossId()), tgraph.crosses.get(road.getEndCrossId()));
            if(r.start !=null) r.start.out.add(r);
            if(r.end !=null) r.end.in.add(r);
            r.updateCount.setToNow(new TimePointInt(0), 0);
            tgraph.roads.put(road.getRoadId(), r);
        }
    }

    public void execute(ImportTemporalDataTx tx){
        for(StatusUpdate s : tx.data){
            RelRoad r = tgraph.roads.get(s.getRoadId());
            TimePointInt time = new TimePointInt(s.getTime());
            r.tpJamStatus.setToNow(time, JamStatus.valueOf(s.getJamStatus()));
            r.tpTravelTime.setToNow(time, s.getTravelTime());
            r.tpSegCount.setToNow(time, (byte) s.getSegmentCount());
            Integer cnt = r.updateCount.get(TimePointInt.Now);
            if(cnt==null){
                r.updateCount.setToNow(time, 1);
            }else{
                r.updateCount.setToNow(time, cnt+1);
            }
        }
    }

    public List<AbstractTransaction> execute(ReachableAreaQueryTx tx){
        List<AbstractTransaction> resultTxArr = new ArrayList<>();
        EarliestArriveTime algo = new EarliestArriveTime(tx.getStartCrossId(), tx.getDepartureTime(), tx.getTravelTime()) {
            @Override
            protected int getEarliestArriveTime(Long roadId, int departureTime) throws UnsupportedOperationException {
                int minArriveTime = Integer.MAX_VALUE;
                RelRoad r = tgraph.roads.get(roadId);
                for(int curT = departureTime; curT<minArriveTime && curT<=endTime; curT++){
                    Integer period = r.tpTravelTime.get( new TimePointInt(curT));
                    if(period == null){
                        if(replaceReachableAreaTx) resultTxArr.add(new EarliestArriveTimeAggrTx(roadId, departureTime, this.endTime, -1, 0));
                        throw new UnsupportedOperationException();
                    }
                    if (curT + period < minArriveTime) {
                        minArriveTime = curT + period;
                    }
                }
                if(replaceReachableAreaTx){
                    int updateCnt = r.updateCount.get(new TimePointInt(endTime)) - r.updateCount.get(new TimePointInt(departureTime));
                    resultTxArr.add(new EarliestArriveTimeAggrTx(roadId, departureTime, this.endTime, minArriveTime, updateCnt));
                }
                return minArriveTime;
            }

            @Override
            protected Iterable<Long> getAllOutRoads(long nodeId) {
                List<Long> rids = new ArrayList<>();
                for(RelRoad road : tgraph.crosses.get(nodeId).out){
                    rids.add(road.id);
                }
                rids.sort(null);
                if(replaceReachableAreaTx) resultTxArr.add(new NodeNeighborRoadTx(nodeId, rids));
                return rids;
            }

            @Override
            protected long getEndNodeId(long roadId) {
                return tgraph.roads.get(roadId).end.id;
            }
        };
        List<EarliestArriveTime.NodeCross> answer = new ArrayList<>(algo.run());
        System.out.println("result size: "+answer.size());
        answer.sort(Comparator.comparingLong(EarliestArriveTime.NodeCross::getId));
        ReachableAreaQueryTx.Result result = new ReachableAreaQueryTx.Result();
        result.setNodeArriveTime(answer);
        tx.setResult(result);
        if(!replaceReachableAreaTx) resultTxArr.add(tx);
        return resultTxArr;
    }
}
