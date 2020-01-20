package org.act.tgraph.demo.benchmark;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import org.act.tgraph.demo.benchmark.client.DBProxy;
import org.act.tgraph.demo.benchmark.client.SqlServerExecutorClient;
import org.act.tgraph.demo.benchmark.client.TGraphExecutorClient;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;
import org.act.tgraph.demo.utils.Helper;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class BenchmarkRunner {

    public static void main(String[] args) throws Exception {
        BenchmarkWriter writer;
        BenchmarkReader reader;

//        TrafficTemporalPropertyGraph tgraph = new TrafficTemporalPropertyGraph();
//        tgraph.importTopology(new File("/tmp/road_topology.csv.gz"));
//        writer = new BenchmarkWriter("/tmp/benchmark.gz");
//        BenchmarkTxArgsGenerator argsGen = new BenchmarkTxArgsGenerator();
//        writer.write(argsGen.phaseImportStatic(tgraph));
//        writer.write(argsGen.phaseWriteTemporalProp(1000, Helper.downloadTrafficFiles("/tmp/traffic", "0501", "0503")));
//        writer.write(argsGen.phaseRead(Helper.monthDayStr2TimeInt("0501"), Helper.monthDayStr2TimeInt("0503"), 10));
//        writer.close();
//
//        BenchmarkTxResultGenerator resultGen = new BenchmarkTxResultGenerator();
//        reader = new BenchmarkReader("/tmp/benchmark.gz");
//        writer = new BenchmarkWriter("/tmp/benchmark-with-result.gz");
//        while(reader.hasNext()){
//            writer.write(resultGen.execute(reader.next()));
//        }
//        reader.close();
//        writer.close();

        reader = new BenchmarkReader("/tmp/benchmark-with-result.gz");

        DBProxy client = new TGraphExecutorClient("localhost", 1, 800);
//        DBProxy client = new SqlServerExecutorClient("localhost", 1, 800);
        String serverName = client.testServerClientCompatibility();
        BenchmarkTxResultProcessor validator = new BenchmarkTxResultProcessor(Helper.getLogger(),"test-test", serverName);
        client.createDB();
        while(reader.hasNext()){
            AbstractTransaction tx = reader.next();
            validator.process(client.execute(tx), tx);
        }
        reader.close();
        client.close();
    }
}
