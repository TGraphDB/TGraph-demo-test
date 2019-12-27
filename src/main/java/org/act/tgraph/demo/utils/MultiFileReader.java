package org.act.tgraph.demo.utils;

import com.google.common.collect.AbstractIterator;
import org.act.tgraph.demo.model.StatusUpdate;

import java.io.*;
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
                curReader = new BufferedReader(new FileReader(file));
                String line = curReader.readLine();
                return new StatusUpdate(line);
            }else{
                String line = curReader.readLine();
                if(line==null) {
                    Future<File> f = files.poll();
                    if(f==null) return endOfData();
                    File file = f.get();
                    curReader = new BufferedReader(new FileReader(file));
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
            if(file.exists()) return file;
            File compressedFile = new File(file.getParent(), file.getName() + ".gz");
            if(compressedFile.exists()) return DataDownloader.decompressGZip(compressedFile, file);
            DataDownloader.download("http://amitabha.water-crystal.org/TGraphDemo/bj-traffic/"+compressedFile.getName(), compressedFile);
            return DataDownloader.decompressGZip(compressedFile, file);
        }
    }
}
