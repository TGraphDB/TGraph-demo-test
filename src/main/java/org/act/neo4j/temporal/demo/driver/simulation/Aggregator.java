package org.act.neo4j.temporal.demo.driver.simulation;

/**
 * Created by song on 16-2-4.
 */
abstract public class Aggregator {
    abstract public String result();

    abstract public boolean add(int time, Object value);

    private static int compareBytes(byte[] a, byte[] b){
        int lim = Math.min(a.length, b.length);
        int k = 0;
        while (k < lim) {
            byte c1 = a[k];
            byte c2 = b[k];
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return a.length - b.length;
    }

    public static Aggregator MIN_VALUE = new Aggregator(){


        private byte[] min;

        @Override
        public String result() {
            return new String(min);
        }

        @Override
        public boolean add(int time, Object value) {
            byte[] v = (byte[])value;
            if(this.min==null){
                this.min = v;
            }else{
                int result = compareBytes(this.min, v);
                if(result>0){
                    this.min = v;
                }
            }
            return true;
        }
    };

    public static Aggregator MAX_VALUE = new Aggregator(){
        private byte[] max;

        @Override
        public String result() {
            return new String(max);
        }

        @Override
        public boolean add(int time, Object value) {
            byte[] v = (byte[])value;
            if(this.max==null){
                this.max = v;
            }else{
                int result = compareBytes(this.max, v);
                if(result<0){
                    this.max = v;
                }
            }
            return true;
        }
    };

    public static Aggregator COUNT = new Aggregator(){
        private int count;

        @Override
        public String result() {
            return count+"";
        }

        @Override
        public boolean add(int time, Object value) {
            count++;
            return true;
        }
    };

    public static Aggregator MAX_TIME = new Aggregator(){
        private int time;

        @Override
        public String result() {
            return time+"";
        }

        @Override
        public boolean add(int time, Object value) {
            if(this.time==0){
                this.time=time;
            }else{
                if(this.time<time){
                    this.time = time;
                }
            }
            return true;
        }
    };

    public static Aggregator MIN_TIME = new Aggregator(){
        private int time;

        @Override
        public String result() {
            return time+"";
        }

        @Override
        public boolean add(int time, Object value) {
            if(this.time==0){
                this.time=time;
            }else{
                if(this.time>time){
                    this.time = time;
                }
            }
            return true;
        }
    };

    public static Aggregator AVG_TIME = new Aggregator(){
        private int timeSum;
        private int count;

        @Override
        public String result() {
            return timeSum/new Float(count) +"";
        }

        @Override
        public boolean add(int time, Object value) {
            this.timeSum += time;
            count++;
            return true;
        }
    };
}
