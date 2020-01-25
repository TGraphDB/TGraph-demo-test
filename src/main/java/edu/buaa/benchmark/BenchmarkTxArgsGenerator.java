package edu.buaa.benchmark;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import edu.buaa.algo.EarliestArriveTime;
import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.benchmark.transaction.ImportStaticDataTx;
import edu.buaa.benchmark.transaction.ImportTemporalDataTx;
import edu.buaa.benchmark.transaction.ReachableAreaQueryTx;
import edu.buaa.model.*;
import edu.buaa.utils.Helper;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Generate an instance of benchmark (a iterator/list of transactions) from given arguments.
 */
public class BenchmarkTxArgsGenerator {
    public static void main(String[] args){
        String workDir = Helper.mustEnv("WORK_DIR");
        String benchmarkFileName = Helper.mustEnv("BENCHMARK_FILE_OUTPUT");
        int temporalDataPerTx = Integer.parseInt(Helper.mustEnv("TEMPORAL_DATA_PER_TX"));
        String temporalDataStartT = Helper.mustEnv("TEMPORAL_DATA_START");
        String temporalDataEndT = Helper.mustEnv("TEMPORAL_DATA_END");
        int reachableAreaTxCnt = Integer.parseInt(Helper.mustEnv("REACHABLE_AREA_TX_CNT"));

        try {
            TrafficTemporalPropertyGraph tgraph = new TrafficTemporalPropertyGraph();
            tgraph.importTopology(new File(workDir, "road_topology.csv.gz"));
            Helper.downloadTrafficFiles(workDir, temporalDataStartT, temporalDataEndT);

            BenchmarkTxArgsGenerator gen = new BenchmarkTxArgsGenerator();
            BenchmarkWriter writer = new BenchmarkWriter(new File(workDir, benchmarkFileName + ".gz"));
            writer.write(gen.phaseImportStatic(tgraph));
            writer.write(gen.phaseWriteTemporalProp(temporalDataPerTx, Helper.trafficFileList(workDir, temporalDataStartT, temporalDataEndT)));
            writer.write(gen.phaseRead(Helper.monthDayStr2TimeInt(temporalDataStartT), Helper.monthDayStr2TimeInt(temporalDataEndT), reachableAreaTxCnt));
            writer.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private Map<CrossNode, Long> crossIdMap = new HashMap<>();
    private Map<String, Long> roadIdMap = new HashMap<>();
    private Random rand = new Random();

    //
    public AbstractTransaction phaseImportStatic(TrafficTemporalPropertyGraph tgraph){
        final List<ImportStaticDataTx.StaticCrossNode> crosses = new ArrayList<>();
        final List<ImportStaticDataTx.StaticRoadRel> roads = new ArrayList<>();
        long crossId = 0, roadId = 0;
        for(CrossNode cross : tgraph.getAllCross()){
            ImportStaticDataTx.StaticCrossNode node = new ImportStaticDataTx.StaticCrossNode();
            node.setId(crossId);
            node.setName(cross.name);
            crosses.add(node);
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

    public Iterator<AbstractTransaction> phaseWriteTemporalProp(int linePerTx, List<File> files) throws IOException {
        return new WriteTemporalDataTxIterator(linePerTx, files);
    }

    public Iterator<AbstractTransaction> phaseRead(int startT, int endT, int txCount){
        return new ReadTxIterator(startT, endT, txCount);
    }

//    public Iterator<AbstractTransaction> phaseReadWrite(){
//
//    }

    private class WriteTemporalDataTxIterator extends AbstractIterator<AbstractTransaction> {
        final int linePerTx;
        final Iterator<File> fileIter;
        BufferedReader br;
        WriteTemporalDataTxIterator(int linePerTx, List<File> files) throws IOException {
            this.linePerTx = linePerTx;
            this.fileIter = files.iterator();
            Preconditions.checkArgument(fileIter.hasNext());
            this.br = Helper.gzipReader(fileIter.next());
        }
        @Override
        protected AbstractTransaction computeNext() {
            try {
                AbstractTransaction result = readFile(br);
                if(result==null && fileIter.hasNext()){
                    if(this.br!=null) br.close();
                    this.br = Helper.gzipReader(fileIter.next());
                    result = readFile(br);
                    Preconditions.checkArgument(result!=null, "should not happen!");
                    return result;
                }else if(result==null){
                    this.br.close();
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

    private class ReadTxIterator extends AbstractIterator<AbstractTransaction> {
        private final int startTime;
        private final int endTime;
        private final int txCount;
        private int curCnt = 0;

        ReadTxIterator(int startT, int endT, int txCount){
            this.startTime = startT;
            this.endTime = endT;
            this.txCount = txCount;
        }
        @Override
        protected AbstractTransaction computeNext() {
            if(curCnt>=txCount) return endOfData();
            curCnt++;
            long startCrossId = rand.nextInt(crossIdMap.size());
            int departureTime = rand.nextInt(endTime - startTime )+ startTime;
            return new ReachableAreaQueryTx(startCrossId, departureTime, 1800);
        }
    }

}
