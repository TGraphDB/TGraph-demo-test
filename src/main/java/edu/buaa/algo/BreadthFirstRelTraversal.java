package edu.buaa.algo;

import com.google.common.collect.AbstractIterator;
import edu.buaa.model.RoadRel;
import edu.buaa.model.TrafficTemporalPropertyGraph;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class BreadthFirstRelTraversal extends AbstractIterator<RoadRel> {
    private Set<RoadRel> isVisited = new HashSet<>();
    private LinkedList<RoadRel> toVisit = new LinkedList<>();

    public BreadthFirstRelTraversal(RoadRel road) {
        toVisit.add(road);
    }

    @Override
    protected RoadRel computeNext() {
        RoadRel road = toVisit.poll();
        if(road==null) return endOfData();
        road.inChains.forEach(neighbor -> {
            if(!isVisited.contains(neighbor)){
                toVisit.add(neighbor);
                isVisited.add(neighbor);
            }
        });
        road.outChains.forEach(neighbor -> {
            if(!isVisited.contains(neighbor)){
                toVisit.add(neighbor);
                isVisited.add(neighbor);
            }
        });
        return road;
    }
}
