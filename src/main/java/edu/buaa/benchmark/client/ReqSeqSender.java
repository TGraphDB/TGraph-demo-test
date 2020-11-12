package edu.buaa.benchmark.client;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import edu.buaa.benchmark.client.DBProxy.ServerResponse;

import java.util.*;
import java.util.concurrent.*;

class ReqSeqSender{
    private volatile boolean shouldRun = true;
    private int parallelCnt;
    private Map<Integer, LinkedBlockingQueue<ReqFuture<ServerResponse>>> req2send = Collections.synchronizedMap(new HashMap<>());
    private LinkedList<Thread> threads = new LinkedList<>();

    ReqSeqSender(int parallel){
        this.parallelCnt = parallel;
        for(int i=0; i<parallel; i++){
            int finalI = i;
            req2send.putIfAbsent(finalI, new LinkedBlockingQueue<>());
            threads.add(new Thread(()->{
                LinkedBlockingQueue<ReqFuture<ServerResponse>> queue = req2send.get(finalI);
                while(shouldRun) try{
                    ReqFuture<ServerResponse> req = queue.take();
                    if(req instanceof CloseThreadFuture) return;
                    req.call();
                } catch (InterruptedException e) {
                    System.out.println("thread interrupted.");
                } catch (Exception e) {
                    System.out.println("tx execute err: "+ e.getMessage());
                }
            }));
        }
    }
    ListenableFuture<ServerResponse> submit(Callable<ServerResponse> req, int bucket){
        ReqFuture<ServerResponse> f = new ReqFuture<>(req);
        req2send.get(bucket).add(f);
        return f;
    }

    static class ReqFuture<T> extends AbstractFuture<T>{
        private Callable<T> req;
        public ReqFuture(Callable<T> task) {
            this.req = task;
        }
        public void call() throws Exception {
            this.set(req.call());
        }
    }

    static class CloseThreadFuture<T> extends ReqFuture<T>{
        public CloseThreadFuture(Callable<T> task) {
            super(task);
        }
        public void call() throws Exception {
            throw new Exception("thread req send done, exit");
        }
    }

    public void awaitDone() throws InterruptedException {
//        shouldRun = false;
        HashSet<Integer> sentExit = new HashSet<>();
        while(true){
            for(int i=0; i<parallelCnt; i++){
                LinkedBlockingQueue<ReqFuture<ServerResponse>> q = req2send.get(i);
                if(q.isEmpty() && !sentExit.contains(i)) {
                    q.add(new CloseThreadFuture<>(null));
                    sentExit.add(i);
                }
            }
            if(sentExit.size()==parallelCnt) break;
            Thread.sleep(100);
        }
        for(Thread t : threads){
            t.join();
        }
    }
}
