package org.act.tgraph.demo.benchmark;

import org.act.tgraph.demo.benchmark.client.TGraphExecutorClient;
import org.act.tgraph.demo.client.Config;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class BenchmarkRunner {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        BenchmarkWriter writer;
        BenchmarkReader reader;

//        TrafficTemporalPropertyGraph tgraph = new TrafficTemporalPropertyGraph();
//        tgraph.importTopology(new File("/tmp/road_topology.csv.gz"));
////        tgraph.importTraffic(Helper.trafficFileList("/tmp/traffic", "0501", "0507"));
//        writer = new BenchmarkWriter("/tmp/benchmark.gz");
//        BenchmarkTxArgsGenerator argsGen = new BenchmarkTxArgsGenerator();
//        writer.write(argsGen.phaseImportStatic(tgraph));
//        writer.write(argsGen.phaseWriteTemporalProp(1000, Helper.trafficFileList("/tmp/traffic", "0501", "0503")));
//        writer.write(argsGen.phaseRead(Helper.monthDayStr2TimeInt("0501"), Helper.monthDayStr2TimeInt("0503"), 10));
//        writer.close();

//        BenchmarkTxResultGenerator resultGen = new BenchmarkTxResultGenerator();
//        reader = new BenchmarkReader("/tmp/benchmark.gz");
//        writer = new BenchmarkWriter("/tmp/benchmark-with-result.gz");
//        while(reader.hasNext()){
//            writer.write(resultGen.execute(reader.next()));
//        }
//        reader.close();
//        writer.close();

        reader = new BenchmarkReader("/tmp/benchmark-with-result.gz");
        BenchmarkTxResultProcessor validator = new BenchmarkTxResultProcessor(Config.sjh.getLogger(),"test-test");
        TGraphExecutorClient client = new TGraphExecutorClient("localhost", 1, 800, validator);
        while(reader.hasNext()){
            client.execute(reader.next());
        }
        reader.close();
        client.close();
    }
}
