package org.act.tgraph.demo.utils;

import com.google.common.collect.AbstractIterator;
import org.act.tgraph.demo.model.StatusUpdate;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 */
public class MultiFileReader extends AbstractIterator<StatusUpdate> implements Closeable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final LinkedList<Future<File>> files = new LinkedList<>();
    private BufferedReader curReader;

    public MultiFileReader(List<File> fileList) {
        for(File file : fileList){
            TrafficFileProcessingTask task = new TrafficFileProcessingTask(file);
            Future<File> future = executor.submit(task);
            files.add(future);
        }
    }

    @Override
    protected StatusUpdate computeNext() {
        try {
            if(curReader==null){
                Future<File> f = files.poll();
                if(f==null) return endOfData();
                File file = f.get();
                curReader = Helper.gzipReader(file);
                String line = curReader.readLine();
                return new StatusUpdate(line);
            }else{
                String line = curReader.readLine();
                if(line==null) {
                    Future<File> f = files.poll();
                    if(f==null) return endOfData();
                    File file = f.get();
                    curReader = Helper.gzipReader(file);
                    line = curReader.readLine();
                    return new StatusUpdate(line);
                }else{
                    return new StatusUpdate(line);
                }
            }
        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("fail to read traffic file.");
    }

    public void close(){
        executor.shutdown();
    }


    private static class TrafficFileProcessingTask implements Callable<File> {
        private final File file;
        public TrafficFileProcessingTask(File file) {
            this.file = file;
        }
        @Override
        public File call() throws Exception {
            if(!file.exists()) Helper.download("http://amitabha.water-crystal.org/TGraphDemo/bj-traffic/"+file.getName(), file);
            return file;
        }
    }
}
