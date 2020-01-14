package org.act.tgraph.demo.benchmark;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;
import org.act.tgraph.demo.benchmark.transaction.ImportStaticDataTx;
import org.act.tgraph.demo.benchmark.transaction.ImportTemporalDataTx;
import org.act.tgraph.demo.model.CrossNode;
import org.act.tgraph.demo.model.RoadRel;
import org.act.tgraph.demo.model.StatusUpdate;
import org.act.tgraph.demo.model.TrafficTemporalPropertyGraph;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

/**
 * Generate an instance of benchmark (a iterator/list of transactions) from given arguments.
 */
public class BenchmarkGenerator {
    private Map<CrossNode, Long> crossIdMap = new HashMap<>();
    private Map<String, Long> roadIdMap = new HashMap<>();

    public BenchmarkGenerator(){

    }

    //
    public AbstractTransaction phaseImportStatic(TrafficTemporalPropertyGraph tgraph){
        final List<Pair<Long, String>> crosses = new ArrayList<>();
        final List<ImportStaticDataTx.StaticRoadRel> roads = new ArrayList<>();
        long crossId = 0, roadId = 0;
        for(CrossNode cross : tgraph.getAllCross()){
            crosses.add(Pair.of(crossId, cross.id));
            crossIdMap.put(cross, crossId);
            crossId++;
        }
        for(RoadRel r: tgraph.getAllRoads()){
            roads.add(new ImportStaticDataTx.StaticRoadRel(
                    roadId,
                    crossIdMap.get(tgraph.getRoadEndCross(r)),
                    crossIdMap.get(tgraph.getRoadStartCross(r)),
                    r.id, r.length, r.angle, r.getType()));
            roadIdMap.put(r.id, roadId);
            roadId++;
        }
        return new ImportStaticDataTx(crosses, roads);
    }

    public Iterator<AbstractTransaction> phaseWriteTemporalProp(int linePerTx, List<File> files) throws FileNotFoundException {
        return new WriteTemporalDataTxIterator(linePerTx, files);
    }

    public Iterator<AbstractTransaction> phaseRead(){

    }

    public Iterator<AbstractTransaction> phaseReadWrite(){

    }

    private class WriteTemporalDataTxIterator extends AbstractIterator<AbstractTransaction> {
        final int linePerTx;
        final Iterator<File> fileIter;
        BufferedReader br;
        WriteTemporalDataTxIterator(int linePerTx, List<File> files) throws FileNotFoundException {
            this.linePerTx = linePerTx;
            this.fileIter = files.iterator();
            assert fileIter.hasNext();
            this.br = new BufferedReader(new FileReader(fileIter.next()));
        }
        @Override
        protected AbstractTransaction computeNext() {
            try {
                AbstractTransaction result = readFile(br);
                if(result==null && fileIter.hasNext()){
                    this.br = new BufferedReader(new FileReader(fileIter.next()));
                    result = readFile(br);
                    assert result!=null;
                    return result;
                }else if(result==null){
                    return endOfData();
                }else{
                    return result;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return endOfData();
            }
        }

        private AbstractTransaction readFile(BufferedReader br) throws IOException {
            List<ImportTemporalDataTx.StatusUpdate> lines = new ArrayList<>();
            int i=0;
            String line;
            while((line=br.readLine())!=null && i<linePerTx){
                StatusUpdate s = new StatusUpdate(line);
                lines.add(new ImportTemporalDataTx.StatusUpdate(
                        roadIdMap.get(s.roadId),
                        s.time, s.travelTime, s.jamStatus, s.segmentCount
                ));
                i++;
            }
            if(i>0){
                return new ImportTemporalDataTx(lines);
            }else{
                return null;
            }
        }
    }

}
