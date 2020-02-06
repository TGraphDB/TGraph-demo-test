package edu.buaa.benchmark;

import com.google.common.collect.Iterators;
import edu.buaa.algo.EarliestArriveTime;
import edu.buaa.benchmark.transaction.*;
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
        boolean genResult = Boolean.parseBoolean(Helper.mustEnv("GENERATE_RESULT_VERIFY_TX"));
        String benchmarkInputFileName = Helper.mustEnv("BENCHMARK_FILE_INPUT");
        String benchmarkOutputFileName = Helper.mustEnv("BENCHMARK_FILE_OUTPUT");

        try {
            BenchmarkTxResultGenerator resultGen = new BenchmarkTxResultGenerator(genResult);
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

    private final boolean verifyTxs;
    private TrafficTGraph tgraph = new TrafficTGraph();

    public BenchmarkTxResultGenerator(boolean generateVerifyTxs) {
        this.verifyTxs = generateVerifyTxs;
    }

    public List<AbstractTransaction> execute(AbstractTransaction tx) throws IOException {
        if(tx instanceof ImportTemporalDataTx){
            execute((ImportTemporalDataTx) tx);
        }else if(tx instanceof ReachableAreaQueryTx){
            return execute((ReachableAreaQueryTx) tx);
        }else if(tx instanceof ImportStaticDataTx){
            execute((ImportStaticDataTx) tx);
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
            tgraph.roads.put(road.getRoadId(), r);
        }
    }

    public void execute(ImportTemporalDataTx tx){
        for(ImportTemporalDataTx.StatusUpdate s : tx.data){
            RelRoad r = tgraph.roads.get(s.getRoadId());
            TimePointInt time = new TimePointInt(s.getTime());
            r.tpJamStatus.setToNow(time, JamStatus.valueOf(s.getJamStatus()));
            r.tpTravelTime.setToNow(time, s.getTravelTime());
            r.tpSegCount.setToNow(time, (byte) s.getSegmentCount());
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
                        if(verifyTxs) resultTxArr.add(new EarliestArriveTimeAggrTx(roadId, departureTime, this.endTime, -1));
                        throw new UnsupportedOperationException();
                    }
                    if (curT + period < minArriveTime) {
                        minArriveTime = curT + period;
                    }
                }
                if(verifyTxs) resultTxArr.add(new EarliestArriveTimeAggrTx(roadId, departureTime, this.endTime, minArriveTime));
                return minArriveTime;
            }

            @Override
            protected Iterable<Long> getAllOutRoads(long nodeId) {
                List<Long> rids = new ArrayList<>();
                for(RelRoad road : tgraph.crosses.get(nodeId).out){
                    rids.add(road.id);
                }
                rids.sort(null);
                if(verifyTxs) resultTxArr.add(new NodeNeighborRoadTx(nodeId, rids));
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
        resultTxArr.add(tx);
        return resultTxArr;
    }
}
