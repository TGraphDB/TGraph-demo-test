package org.act.tgraph.demo.benchmark;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.AbstractIterator;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;
import org.act.tgraph.demo.benchmark.transaction.ImportStaticDataTx;
import org.act.tgraph.demo.benchmark.transaction.ImportTemporalDataTx;
import org.act.tgraph.demo.benchmark.transaction.ReachableAreaQueryTx;
import org.act.tgraph.demo.utils.Helper;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class BenchmarkReader extends AbstractIterator<AbstractTransaction> {

    private final BufferedReader reader;

    public BenchmarkReader(String file) throws IOException {
        reader = Helper.gzipReader(new File(file));
    }

    @Override
    protected AbstractTransaction computeNext() {
        try {
            String line = reader.readLine();
            if(line==null) return endOfData();
            JsonObject obj = Json.parse(line).asObject();
            switch (AbstractTransaction.TxType.valueOf(obj.get("type").asString())){
                case tx_import_static_data: return new ImportStaticDataTx(obj);
                case tx_import_temporal_data: return new ImportTemporalDataTx(obj);
                case tx_query_reachable_area: return new ReachableAreaQueryTx(obj);
                default: throw new RuntimeException("invalid tx type");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return endOfData();
        }
    }

    public void close() throws IOException {
        reader.close();
    }
}
