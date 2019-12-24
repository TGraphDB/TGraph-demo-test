package org.act.tgraph.demo.algo;

import com.google.common.collect.Lists;
import org.act.tgraph.demo.client.Config;
import org.act.tgraph.demo.client.utils.TransactionWrapper;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.temporal.TimePoint;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.slf4j.LoggerFactory;

/**
 * This is actually a Time Dependent Dijkstra shortest path algorithm.
 * Created by song on 16-4-25.
 */
public class TDAStar {
    public static abstract class AlgoHookAction{
        abstract public boolean run(long nodeId);
    }

    private Logger logger;
    private GraphDatabaseService db;


    public TDAStar(GraphDatabaseService db, Logger logger){
        this.db = db;
        this.logger = logger;
    }
    public static void main(String[] args){
        Logger logger = LoggerFactory.getLogger(TDAStar.class);
        logger.info("============= begin execution =============");
        Config config = new Config();
        TDAStar algo = new TDAStar(new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(new File(config.dbPath))
                .loadPropertiesFromFile("")
                .newGraphDatabase(),logger);
        algo.go(3,10000,1011070322,null);
        logger.info("=============  end  execution =============");
        System.exit(0);
    }

    /**
     * use priority queue as min heap, this guarantee:
     * 1. O(1) for "peek(get node with min g value among queue)" operation
     * 2. O(log(n)) for "add" and "remove" operation(since it will adjust the heap)
     */
    PriorityQueue<Node> minHeap = new PriorityQueue<Node>(10000000, new Comparator<Node>() {
        @Override
        public int compare(Node o1, Node o2) {
            int g1 = (Integer) o1.getProperty("algo-astar-G");
            int g2 = (Integer) o2.getProperty("algo-astar-G");
            if(g1< g2) return -1;
            if(g1==g2) return 0;
            else return 1;
        }
    });


    public void go(long from, long to, int t0, AlgoHookAction hookAction){
        logger.info("from [" + from + "] to [" + to + "] at time [" + t0 + "]");
        try{
            initAlgo(from, to, t0);
            long searchCount=0;
            long node;
            while((node = findSmallestClosedNode())!=to){
                if(hookAction!=null && !hookAction.run(node)) break;
                loopAllNeighborsUpdateGValue(node);
                searchCount++;
            }
            logger.info("search " + searchCount + " nodes");
            printPath(from, to);
        } catch(RuntimeException e){
            e.printStackTrace();
        } catch (Exception e){
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally{
            db.shutdown();
        }
    }

    private void printPath(final long from, final long to) {
        new TransactionWrapper() {
            public void runInTransaction() {
                List<Long> rPath = new ArrayList<Long>();
                long parent=to;
                while((parent = getParent(parent))!=from){
                    rPath.add(parent);
                }
                rPath.add(from);
                List<Long> path = Lists.reverse(rPath);
                for(long node:path){
                    System.out.print(node+"["+getGvalue(node)+"]->");
                }
                logger.info(to + "[" + getGvalue(to) + "]");
                logger.info("path length: " + path.size());
            }
        }.start(db);
    }

    private int getGvalue(long nodeId) {
        return getGvalue(db.getNodeById(nodeId));
    }
    private int getGvalue(Node node) {
        return (Integer) node.getProperty("algo-astar-G");
    }

    /**
     * get parent node, must used in transaction.
     * @param me current node id
     * @return parent node id
     */
    private long getParent(long me) {
        return db.getNodeById((Long)db.getNodeById(me).getProperty("algo-astar-parent")).getId();
    }

    /**
     * loop through all neighbors of a given node,
     * and for each neighbor node:
     * 1. update its G value
     * 2. set parent to source node
     * 3. mark node status to CLOSE
     * after the loop above, mark given node status to FINISH
     * @param nodeId given node's id
     */
    private void loopAllNeighborsUpdateGValue(final long nodeId) {
        new TransactionWrapper(){
            public void runInTransaction(){
                Node node = db.getNodeById(nodeId);
                int g = getGvalue(node);
                for(Relationship r : node.getRelationships()){
                    Node neighbor = r.getOtherNode(node);
//                    logger.info(r.getDynPropertyPointValue("travel-time", g));
                    Object travelTimeObj = r.getTemporalProperty("travel-time", new TimePoint(g));
                    int travelTime;
                    if(travelTimeObj!=null) {
                        travelTime = (Integer) travelTimeObj;
                    }else{
//                        logger.info(r.getId()+" "+g);
                        travelTime = 5;
                    }
                    switch (getStatus(neighbor)){
                        case OPEN:
                            setG(neighbor, travelTime + g);
                            setStatus(neighbor,Status.CLOSE);
                            setParent(neighbor, node);
                            break;
                        case CLOSE:
                            int gNeighbor = (Integer) neighbor.getProperty("algo-astar-G");
                            if(gNeighbor>g+travelTime){
                                setG(neighbor,g+travelTime);
                                setParent(neighbor,node);
                            }
                            break;
                    }
                }
                setStatus(node,Status.FINISH);
            }
        }.start(db);
    }

    /**
     * this is an O(1) implementation, because:
     * 1. node status only transfer from OPEN to CLOSE, never back.
     * 2. we maintain a min G Value each time the transfer in 1 happens.
     * BE CAREFUL! this implementation is only valid based on the assumption [1]!
     * therefore it may only work for Dijkstra SP algorithm.
     * RE VALID this when using algorithm other than Dijkstra SP.
     * @return node in set( status == CLOSE ) and has smallest G value among the set.
     */
    private long findSmallestClosedNode() {
        return minHeap.peek().getId();
    }

    /**
     * we do the following:
     * 1. set all node status to OPEN, except source node (CLOSE)
     * 2. set G value of source node to t0
     * 3. set min G value to be t0, min point to source node
     * @param from source node
     * @param to target/destination node
     * @param t0 start from source node at time t0
     */
    private void initAlgo(final long from, long to, final int t0) {
        new TransactionWrapper(){
            @Override
            public void runInTransaction() {
                long nodeCount=0;
                for (Node node : GlobalGraphOperations.at(db).getAllNodes()) {
                    if(node.getId()==from){
                        setStatus(node,Status.CLOSE);
                        logger.info("set node[" + node.getId() + "]property:" + t0);
                        setG(node,t0);
                        minHeap.add(node);
                    }else{
                        setStatus(node, Status.OPEN);
                    }
                    nodeCount++;
                }
                System.out.print("get "+nodeCount+" nodes,");
                long roadCount=0;
                for (Relationship r : GlobalGraphOperations.at(db).getAllRelationships()) {
                    if(r.getStartNode().getId()==from || r.getEndNode().getId()==from){
//                        for(String key: r.getPropertyKeys()){
//                            logger.info(key);
//                        }
//                        logger.info(r.getProperty("grid-id")+":"+
//                                r.getProperty("chain-id")+" "+
//                                r.getDynPropertyPointValue("travel-time", t0));
                    }
                    roadCount++;
                }
                logger.info(" and " + roadCount + " edges");
            }
        }.start(db);
    }

    private void setG(Node node, int value) {
//        new TransactionWrapper(){
//            public void runInTransaction(){
                node.setProperty("algo-astar-G",value);
//            }
//        }.start(db);
    }

    private void setParent(Node node, Node parent) {
//        new TransactionWrapper(){
//            public void runInTransaction(){
                node.setProperty("algo-astar-parent",parent.getId());
//            }
//        }.start(db);
    }

    /**
     * when status transfer from OPEN to CLOSE,
     * will add the node to min G Value heap.
     */
    private void setStatus(Node node, Status status){
//        new TransactionWrapper(){
//            public void runInTransaction(){
                node.setProperty("algo-astar-status",status.value());

//            }
//        }.start(db);
        if (status == Status.CLOSE) {
            minHeap.add(node);
        }
        if( status == Status.FINISH){
            minHeap.remove(node);
        }
    }

    private Status getStatus(final Node node){
        Status result = (Status) new TransactionWrapper() {
            public void runInTransaction() {
                Object status = node.getProperty("algo-astar-status");
                if (status != null) {
                    setReturnValue(Status.valueOf((Integer) status));
                } else {
                    throw new RuntimeException("node property algo-astar-status null");
                }
            }
        }.start(db).getReturnValue();
        return result;
    }

    public enum Status{
        OPEN(0),CLOSE(1),FINISH(2);
        private int value;
        Status(int value){
            this.value=value;
        }
        public int value(){
            return this.value;
        }

        public static Status valueOf(int status) {
            switch (status){
                case 0: return OPEN;
                case 1: return CLOSE;
                case 2: return FINISH;
            }
            throw new RuntimeException("no such value in Status!");
        }
    }


}
