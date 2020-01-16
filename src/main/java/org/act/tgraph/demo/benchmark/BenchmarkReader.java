package org.act.tgraph.demo.benchmark;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.AbstractIterator;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

public class BenchmarkReader extends AbstractIterator<AbstractTransaction> {

    private final BufferedReader reader;

    public BenchmarkReader(String file) throws IOException {
        reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
    }

    @Override
    protected AbstractTransaction computeNext() {
        try {
            String line = reader.readLine();
            JsonObject obj = Json.parse(line).asObject();
            switch (AbstractTransaction.TxType.valueOf(obj.get("type").asString())){
                case tx_import_static_data:

            }
        } catch (IOException e) {
            e.printStackTrace();
            return endOfData();
        }
    }
}
