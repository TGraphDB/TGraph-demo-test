package org.act.tgraph.demo.benchmark;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import org.act.tgraph.demo.algo.EarliestArriveTime;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;
import org.act.tgraph.demo.benchmark.transaction.ImportStaticDataTx;
import org.act.tgraph.demo.benchmark.transaction.ImportTemporalDataTx;
import org.act.tgraph.demo.benchmark.transaction.ReachableAreaQueryTx;
import org.act.tgraph.demo.model.TimePointInt;
import org.act.tgraph.demo.model.TrafficTGraph;
import org.act.tgraph.demo.model.TrafficTGraph.JamStatus;
import org.act.tgraph.demo.model.TrafficTGraph.NodeCross;
import org.act.tgraph.demo.model.TrafficTGraph.RelRoad;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class BenchmarkTxResultGenerator extends AbstractTransactionExecutor {

    private TrafficTGraph tgraph = new TrafficTGraph();

    @Override
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

    @Override
    public void execute(ImportTemporalDataTx tx){
        for(ImportTemporalDataTx.StatusUpdate s : tx.data){
            RelRoad r = tgraph.roads.get(s.getRoadId());
            TimePointInt time = new TimePointInt(s.getTime());
            r.tpJamStatus.setToNow(time, JamStatus.valueOf(s.getJamStatus()));
            r.tpTravelTime.setToNow(time, s.getTravelTime());
            r.tpSegCount.setToNow(time, (byte) s.getSegmentCount());
        }
    }

    @Override
    public void execute(ReachableAreaQueryTx tx){
        EarliestArriveTime algo = new EarliestArriveTime(tx.getStartCrossId(), tx.getDepartureTime(), tx.getTravelTime()) {
            @Override
            protected int getEarliestArriveTime(Long roadId, int departureTime) throws UnsupportedOperationException {
                int minArriveTime = Integer.MAX_VALUE;
                RelRoad r = tgraph.roads.get(roadId);
                for(int curT = departureTime; curT<minArriveTime; curT++){
                    Integer period = r.tpTravelTime.get( new TimePointInt(curT));
                    if(period == null) throw new UnsupportedOperationException();
                    if (curT + period < minArriveTime) {
                        minArriveTime = curT + period;
                    }
                }
                return minArriveTime;
            }

            @Override
            protected Iterable<Long> getAllOutRoads(long nodeId) {
                return ()-> Iterators.transform(tgraph.crosses.get(nodeId).out.iterator(), road -> road.id);
            }

            @Override
            protected long getEndNodeId(long roadId) {
                return tgraph.roads.get(roadId).end.id;
            }
        };
        List<EarliestArriveTime.NodeCross> answer = new ArrayList<>(algo.run());
        System.out.println("result size: "+answer.size());
        answer.sort(Comparator.comparingLong(o -> o.id));
        ReachableAreaQueryTx.Result result = new ReachableAreaQueryTx.Result();
        result.setNodeArriveTime(answer);
        tx.setResult(result);
    }

    public Iterator<AbstractTransaction> eval(Iterator<AbstractTransaction> transactionIterator) {
        return new AbstractIterator<AbstractTransaction>() {
            @Override
            protected AbstractTransaction computeNext() {
                if(transactionIterator.hasNext()) {
                    try {
                        return execute(transactionIterator.next());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return endOfData();
                    }
                }
                else return endOfData();
            }
        };
    }
}
