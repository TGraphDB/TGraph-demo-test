//package simple.tgraph.kernel;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.TypeReference;
//import com.alibaba.fastjson.parser.ParserConfig;
//import edu.buaa.benchmark.client.DBProxy;
//import edu.buaa.benchmark.transaction.AbstractTransaction;
//import edu.buaa.benchmark.transaction.SnapshotQueryTx;
//import edu.buaa.utils.Helper;
//import org.junit.Test;
//
//import java.io.*;
//
//public class SimpleTest {
//
//    @Test
//    public void pairTest() throws IOException {
//        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
//        try(BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\BlackCat\\Desktop\\ddd.txt"))){
//            String big = reader.readLine();
//            DBProxy.ServerResponse res = JSON.parseObject(big, new TypeReference<DBProxy.ServerResponse>() {});
//            SnapshotQueryTx.Result r = (SnapshotQueryTx.Result) res.getResult();
//
//            System.out.println(r.getRoadStatus());
//        }
//    }
//    public static class Pair<L, R> implements Serializable {
//        private L key;
//        private R value;
//        public Pair() {}
//
//        public static <L, R> Pair<L, R> of(L left, R right) {
//            Pair<L, R> p = new Pair();
//            p.key = left;
//            p.value = right;
//            return p;
//        }
//
//        public L getKey() {
//            return key;
//        }
//
////        public void setKey(L key) {
////            this.key = key;
////        }
//
//        public R getValue() {
//            return value;
//        }
//
////        public void setValue(R value) {
////            this.value = value;
////        }
//
//        public boolean equals(Object obj) {
//            if (obj == this) {
//                return true;
//            } else if (!(obj instanceof Pair)) {
//                return false;
//            } else {
//                Pair<?, ?> other = (Pair)obj;
//                return this.getKey().equals(other.getKey()) && this.getValue().equals(other.getValue());
//            }
//        }
//
//        public int hashCode() {
//            return (this.getKey() == null ? 0 : this.getKey().hashCode()) ^ (this.getValue() == null ? 0 : this.getValue().hashCode());
//        }
//
//        public String toString() { return "" + '(' + this.key + ',' + this.value + ')'; }
//
//    }
//}
