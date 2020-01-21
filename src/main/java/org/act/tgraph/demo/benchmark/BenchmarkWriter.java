package org.act.tgraph.demo.benchmark;


import com.alibaba.fastjson.JSON;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;

import java.io.*;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;

/**
 * an instance of benchmark (a list of transactions)
 */
public class BenchmarkWriter {
    private final GZIPOutputStream writer;

    public BenchmarkWriter(File file) throws IOException {
        writer = new GZIPOutputStream(new FileOutputStream(file));
    }

    public void write(AbstractTransaction tx) throws IOException {
        writer.write(JSON.toJSONString(tx).getBytes());
        writer.write('\n');
    }

    public void write(Iterator<AbstractTransaction> txIter) throws IOException {
        while(txIter.hasNext()) write(txIter.next());
    }

    public void close() throws IOException {
        writer.close();
    }
}
