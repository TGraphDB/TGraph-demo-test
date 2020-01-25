package edu.buaa.benchmark;


import com.alibaba.fastjson.JSON;
import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.utils.Helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
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
        writer.write(JSON.toJSONString(tx, Helper.serializerFeatures).getBytes());
        writer.write('\n');
    }

    public void write(List<AbstractTransaction> tx) throws IOException {
        for(AbstractTransaction t : tx){
            write(t);
        }
    }

    public void write(Iterator<AbstractTransaction> txIter) throws IOException {
        while(txIter.hasNext()) write(txIter.next());
    }

    public void close() throws IOException {
        writer.close();
    }
}
