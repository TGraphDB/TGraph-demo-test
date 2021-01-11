package simple.tgraph.kernel.index;

import org.act.temporalProperty.index.IndexValueType;
import org.act.temporalProperty.index.value.rtree.*;
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
        try ( FileChannel readChannel = new FileInputStream( new File( "Z:\\TEMP\\temporal.relationship.properties\\index", "value.000000.index" ) ).getChannel() ) {
            new IndexTableReader( readChannel, new IndexEntryOperator(Collections.singletonList(IndexValueType.INT), 4096 ));

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
            System.out.println(entities.stream().min(Comparator.naturalOrder()).get());
            System.out.println(entities.stream().max(Comparator.naturalOrder()).get());
        }

        private RTreeNode getNode(int pos, RTreeRange bound) {
            map.position(pos);
            RTreeNodeBlock block = new RTreeNodeBlock(map, op);
            RTreeNode node = block.getNode();
            node.setBound(bound);
            return node;
        }

        private void getChildren(RTreeNode node, int pos, int level){
            cnt.merge(level, 1, Integer::sum);
            System.out.print(level+" "+pos+" "+node.isLeaf()+" "+node.getBound()+" ");
            if(node.isLeaf()){
                System.out.println(node.getEntries().size());
                for(IndexEntry entry : node.getEntries()){
                    entities.add(entry.getEntityId());
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
}
