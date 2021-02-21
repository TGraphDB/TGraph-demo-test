package simple.tgraph.kernel.index;

import edu.buaa.utils.Helper;
import org.act.temporalProperty.index.IndexValueType;
import org.act.temporalProperty.index.value.IndexQueryRegion;
import org.act.temporalProperty.index.value.PropertyValueInterval;
import org.act.temporalProperty.index.value.cardinality.HyperLogLog;
import org.act.temporalProperty.index.value.rtree.*;
import org.act.temporalProperty.query.TimePointL;
import org.act.temporalProperty.util.Slice;
import org.act.temporalProperty.util.SliceInput;
import org.act.temporalProperty.util.SliceOutput;
import org.act.temporalProperty.util.TemporalPropertyValueConvertor;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * 测试RTree索引想知道一般有几层，有多少数据点，多少节点，每层的entity id分布如何
 * 结果：3层，数据点数量约4700*341，节点数量第一次1节点，第二层14节点，第三层4700节点，
 * entity id只存在leaf节点上了，中间根本就没存。
 */
public class RTreeIndexTest {
    @Test
    public void readRTreeInfo() throws IOException {
        try ( FileChannel readChannel = new FileInputStream( new File( "D:\\tgraph\\testdb\\temporal.relationship.properties\\index", "value.000000.index" ) ).getChannel() ) {
            new IndexTableReader( readChannel, new IndexEntryOperator(Collections.singletonList(IndexValueType.INT), 4096 ));
//            IndexQueryRegion queryRegion = new IndexQueryRegion(
//                    Helper.time(Helper.timeStr2int("201005020800")),
//                    Helper.time(Helper.timeStr2int("201005021200"))
//            );
//            Slice minValue = TemporalPropertyValueConvertor.toSlice( 100 );
//            Slice maxValue = TemporalPropertyValueConvertor.toSlice( 200 );
//            queryRegion.add(new PropertyValueInterval(0, minValue, maxValue, IndexValueType.INT));
//            RTreeCardinality r = new RTreeCardinality(readChannel, queryRegion, new IndexEntryOperator(Collections.singletonList(IndexValueType.INT), 4096));
//            System.out.println(r.cardinalityEstimator().cardinality());
        }
    }

    public static class IndexTableReader {

        private final IndexEntryOperator op;
        private final MappedByteBuffer map;

        private final Map<Integer, Integer> cnt = new HashMap<>();
        private final Set<Long> entities = new HashSet<>();

        public IndexTableReader( FileChannel channel, IndexEntryOperator indexEntryOperator ) throws IOException {
            this.map = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            this.map.order(ByteOrder.LITTLE_ENDIAN);
//        this.map.flip();
            this.op = indexEntryOperator;

//        System.out.println(map.limit()+" "+map.position()+" "+map.remaining());
            int rootPos = map.getInt();
//        System.out.println(rootPos);
            RTreeRange rootBound = RTreeRange.decode(map, op);

            RTreeNode rootNode = getNode(rootPos, rootBound);
            getChildren(rootNode, rootPos,0);
            System.out.println(cnt);
            System.out.println(entities.size());
//            System.out.println(entities.stream().min(Comparator.naturalOrder()).get());
//            System.out.println(entities.stream().max(Comparator.naturalOrder()).get());
            System.out.println(cntt);
        }

        private RTreeNode getNode(int pos, RTreeRange bound) {
            map.position(pos);
            RTreeNodeBlock block = new RTreeNodeBlock(map, op);
            RTreeNode node = block.getNode();
            node.setBound(bound);
            return node;
        }

        int cntt = 0;

        private void getChildren(RTreeNode node, int pos, int level){
            cnt.merge(level, 1, Integer::sum);
            System.out.print(level+" "+pos+" "+node.isLeaf()+" "+node.getBound()+" ");
//            if(node.getBound().getMax().getEnd().val()>=1272772800) System.out.println("-----");
            if(node.isLeaf()){
                System.out.println(node.getEntries().size());
                for(IndexEntry entry : node.getEntries()){
                    int val = entry.getValue(0).getInt(0);
                    if(entry.getEntityId()==49822){
                        System.out.println("found: "+entry.getStart()+" "+entry.getEnd()+" "+val);
//                        entities.add(entry.getEntityId());
//                        cntt++;
                    }
                }
            }else {
                System.out.println();
                for(RTreeNode diskNode : node.getChildren()){
                    RTreeNode rtNode = getNode(diskNode.getPos(), diskNode.getBound());
                    getChildren(rtNode, diskNode.getPos(), level+1);
                }
            }
        }
    }


    public class RTreeCardinality {
        RTreeNode root;
        List<RTreeNode> firstLevel;

        private RTreeRange queryRegion;
        private IndexEntryOperator op;
        // constructor used for read
        public RTreeCardinality(FileChannel channel, IndexQueryRegion regions, IndexEntryOperator op) throws IOException {
            this.op = op;
            this.queryRegion = op.toRTreeRange(regions);

            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            map.order(ByteOrder.LITTLE_ENDIAN);
            int rootPos = map.getInt();
            System.out.println(rootPos);
            map.position(rootPos);
            new RTreeNodeBlock(map, op);
            System.out.println(map.position());

            int len = map.getInt();
            System.out.println(len);
            byte[] content = new byte[len];
            map.get(content);
            SliceInput in = new Slice(content).input();

            this.root = new RTreeCardinalityNodeBlock(in);
            System.out.println(root.getCardinality());
            this.firstLevel = new ArrayList<>();
            int size = in.readInt();
            for(int i=0; i<size; i++){
                firstLevel.add(new RTreeCardinalityNodeBlock(in));
            }
        }

        public HyperLogLog cardinalityEstimator() {
            HyperLogLog result = HyperLogLog.defaultBuilder();
            System.out.println(queryRegion);
            for(RTreeNode node : firstLevel){
                System.out.println(node.getCardinality()+" "+node.getBound().overlap(queryRegion)+" "+node.getBound());
                if(node.getBound().overlap(queryRegion)) {
                    result.addAll(node.getCardinalityEstimator());
                }
            }
            return result;
        }


        private class RTreeCardinalityNodeBlock extends RTreeNode{

            public RTreeCardinalityNodeBlock(SliceInput in) {
                this.setBound(RTreeRange.decode(in, op));
                this.setCardinalityEstimator(HyperLogLog.decode(in));
            }

            private void setCardinalityEstimator(HyperLogLog decode) {
                this.c = decode;
            }

            @Override
            public boolean isLeaf() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void encode(SliceOutput out) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<RTreeNode> getChildren() {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<IndexEntry> getEntries() {
                throw new UnsupportedOperationException();
            }
        }
    }
}
