package org.act.tgraph.demo.benchmark;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import org.act.tgraph.demo.algo.EarliestArriveTime;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;
import org.act.tgraph.demo.benchmark.transaction.ImportStaticDataTx;
import org.act.tgraph.demo.benchmark.transaction.ImportTemporalDataTx;
import org.act.tgraph.demo.benchmark.transaction.ReachableAreaQueryTx;
import org.act.tgraph.demo.model.TimePointInt;
import org.act.tgraph.demo.model.TrafficTGraph;
import org.act.tgraph.demo.model.TrafficTGraph.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class BenchmarkTxResultGenerator extends AbstractTransactionExecutor {

    private TrafficTGraph tgraph = new TrafficTGraph();

    @Override
    public void execute(ImportStaticDataTx tx){
        for(Pair<Long, String> p : tx.crosses){
            NodeCross n = new NodeCross(p.getLeft(), p.getRight());
            tgraph.crosses.put(p.getLeft(), n);
        }
        for(ImportStaticDataTx.StaticRoadRel road : tx.roads){
            RelRoad r = new RelRoad(road.roadId, road.id, road.length, road.angle, road.type,
                    tgraph.crosses.get(road.startCrossId), tgraph.crosses.get(road.endCrossId));
            if(r.start !=null) r.start.out.add(r);
            if(r.end !=null) r.end.in.add(r);
            tgraph.roads.put(road.roadId, r);
        }
    }

    @Override
    public void execute(ImportTemporalDataTx tx){
        for(ImportTemporalDataTx.StatusUpdate s : tx.data){
            RelRoad r = tgraph.roads.get(s.roadId);
            TimePointInt time = new TimePointInt(s.time);
            r.tpJamStatus.setToNow(time, JamStatus.valueOf(s.jamStatus));
            r.tpTravelTime.setToNow(time, s.travelTime);
            r.tpSegCount.setToNow(time, (byte) s.segmentCount);
        }
    }

    @Override
    public void execute(ReachableAreaQueryTx tx){
        EarliestArriveTime algo = new EarliestArriveTime(tx.startCrossId, tx.departureTime, tx.travelTime) {
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
        List<EarliestArriveTime.NodeCross> result = new ArrayList<>(algo.run());
        System.out.println("result size: "+result.size());
        result.sort(Comparator.comparingLong(o -> o.id));
        JsonObject res = Json.object();
        JsonArray nodeIdArr = Json.array();
        JsonArray arriveTimeArr = Json.array();
        JsonArray parentIdArr = Json.array();
        for(EarliestArriveTime.NodeCross node : result){
            nodeIdArr.add(node.id);
            arriveTimeArr.add(node.arriveTime);
            parentIdArr.add(node.parent);
        }
        res.add("arriveTime", arriveTimeArr);
        res.add("nodeId", nodeIdArr);
        res.add("parentId", parentIdArr);
        tx.setResult(res);
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
