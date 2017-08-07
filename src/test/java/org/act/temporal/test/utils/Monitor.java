package org.act.temporal.test.utils;

import org.slf4j.Logger;

import java.util.List;

/**
 * Created by song on 16-2-23.
 */
public class Monitor {

    private Logger logger;
    private Runtime runtime;

    private long beginTime=0;
    private long endTime=0;
    private long totalMemory=0;
    private long startAppliedMemory=0;
    private long endAppliedMemory=0;
    private long startFreeMemory=0;
    private long endFreeMemory=0;
    private String name;

    public Monitor(Logger logger){
        this.logger=logger;
        runtime = Runtime.getRuntime();
        totalMemory = runtime.maxMemory();
    }

    public void begin(String name){
        startAppliedMemory = runtime.totalMemory();
        startFreeMemory = runtime.freeMemory();
        beginTime = System.currentTimeMillis();
        this.name = name;
    }

    public void begin(){
//        startAppliedMemory = runtime.totalMemory();
//        startFreeMemory = runtime.freeMemory();
        beginTime = System.currentTimeMillis();
    }

    public void end(){
        endTime = System.currentTimeMillis();
//        endAppliedMemory = runtime.totalMemory();
//        endFreeMemory = runtime.freeMemory();
        long totalTime = endTime-beginTime;
        logger.info("[Time Usage]{}: {}ms", this.name, totalTime);
//        logger.info("[Memory Usage] VM max memory:{}, apply:{}, used:{}, free:{}",
//                calcMem(totalMemory), calcMem(endAppliedMemory),
//                calcMem(endAppliedMemory - endFreeMemory), calcMem(endFreeMemory));
    }

    public void end(long count){
        endTime = System.currentTimeMillis();
//        endAppliedMemory = runtime.totalMemory();
//        endFreeMemory = runtime.freeMemory();
        long totalTime = endTime-beginTime;
        logger.info("[Time Usage] Total:{}ms, Average time:{}ms", totalTime, totalTime*1f / count);
//        logger.info("[Memory Usage] VM max memory:{}, apply:{}, used:{}, free:{}",
//                calcMem(totalMemory), calcMem(endAppliedMemory),
//                calcMem(endAppliedMemory - endFreeMemory), calcMem(endFreeMemory));
    }

    private String calcMem(long size){
        long gb=0;
        long mb=0;
        if(size >= 1024*1024*1024){
            gb = size / (1024*1024*1024);
            size = size % (1024*1024*1024);
        }else if(size >= 1024*1024){
            mb = size / (1024*1024);
        }else{
            return "0MB";
        }
        String result="";
        if(gb>0){
            result=gb+"GB";
        }
        if(mb>0){
            result+=mb+"MB";
        }
        return result;
    }



    private List<SnapShot> snapShotList;

    public Monitor(List<SnapShot> list){
        this.snapShotList = list;
    }

    public void snapShot(byte type){
//        switch (type){
//            case 0:
//                logger.info("BEGIN");
//                break;
//            case 1:
//                logger.info("NEXT");
//                break;
//            case 2:
//                logger.info("END");
//                break;
//        }
        //this.snapShotList.add(new SnapShot(type));
    }


    public static byte BEGIN=0;
    public static byte NEXT=1;
    public static byte END=2;


    public static class SnapShot{
        static Runtime runtime;
        long milliSecond,nanoSecond;
//        long appliedMemory,freeMemory;
        byte type;
        SnapShot(byte type){
            this.nanoSecond = System.nanoTime();
            this.milliSecond = System.currentTimeMillis();
            this.type = type;
//            this.appliedMemory = runtime.totalMemory();
//            this.freeMemory = runtime.freeMemory();
        }
        @Override
        public String toString(){
            return this.milliSecond+":"+nanoSecond+":"+type;
        }
    }
}



//    @Test
//    public void createTest(){
//        int n,p,t,v,count = 0;
//        long totalSize;
//        for(n=10;n<=10;n*=10){
//            for(p=10;p<=10000;p*=10){
//                for(t=10;t<=10000;t*=10){
//                    for(v=10;v<=1000000;v*=100){
//                        totalSize=1;
//                        totalSize *= n;
//                        totalSize *= p;
//                        totalSize *= t;
//                        totalSize *= v;
//                        if(totalSize<=1000000000000L){
//                            count++;
//                            try( Transaction tx = db.beginTx() ) {
//                                createTest(n, p, t, v);
//                                tx.success();
//                            }
////                            System.out.printf("Total size:%d,%d,%d,%d=%d\n",n,p,t,v,totalSize);
//                            logger.info("Total size:{},{},{},{}={}", n, p, t, v, totalSize);
//                        }
//                    }
//                }
//            }
//        }
////        System.out.println(count);
//        logger.info("run {} test.",count);
//    }