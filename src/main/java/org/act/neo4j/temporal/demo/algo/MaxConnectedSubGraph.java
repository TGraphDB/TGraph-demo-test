package org.act.neo4j.temporal.demo.algo;

import org.act.neo4j.temporal.demo.Config;
import org.act.neo4j.temporal.demo.utils.TransactionWrapper;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.*;

/**
 * Created by song on 16-5-8.
 */
public class MaxConnectedSubGraph {
    private Config config = new Config();
    private GraphDatabaseService db;

    public static void main(String[]args){
        System.out.println("============= begin execution =============");
        MaxConnectedSubGraph algo = new MaxConnectedSubGraph();
        algo.go();
        System.out.println("=============  end  execution =============");
        System.exit(0);
    }

    public void go() {
//        config.dbPath += "-netOnly";
        initDB();
        final TGraphTraversal traversal = new TGraphTraversal(db);
        // find connected sub-graphs
        final int totalNodes = (Integer) new TransactionWrapper<Integer>(){
            private int countNodes(){
                int nodeCount=0;
                for (Node node : GlobalGraphOperations.at(db).getAllNodes()) {
                    nodeCount++;
                }
                return nodeCount;
            }
            @Override
            public void runInTransaction() {
                setReturnValue(countNodes());
            }
        }.start(db).getReturnValue();

        final Set<Long> visited = new HashSet<Long>();
        final int[] nodeCategoryCount=new int[totalNodes];
        final int[] connectedSubNetId = {0};
        final Map<Integer,Node> categoryStartNodes = new HashMap<Integer, Node>();


        try {
//            new TransactionWrapper() {
//                public void runInTransaction() {
//                    for (Node node : GlobalGraphOperations.at(db).getAllNodes()) {
//                        if (!visited.contains(node.getId())) {
//                            int[] nodeInOneCategory={0};
//                            categoryStartNodes.put(connectedSubNetId[0], node);
//                            traversal.DFS(node, visited, new TGraphTraversal.DFSAction<Node>() {
//                                public boolean visit(Node node) {
//                                    nodeInOneCategory[0]++;
////                                    node.setProperty("connected-subnet-id", connectedSubNetId[0]);
//                                    return true;
//                                }
//                            }, false);
////                            System.out.println("category:" + connectedSubNetId[0] + "has nodes "+nodeInOneCategory[0]+" start node id:"+node.getId());
//                            nodeCategoryCount[connectedSubNetId[0]]=nodeInOneCategory[0];
//                            connectedSubNetId[0]++;
//                        }
//                    }
//                }
//            }.start(db);
            int maxNodeCount=0;
            int maxNodeCategory=0;
//            for(int i=0;i<nodeCategoryCount.length;i++){
//                if(nodeCategoryCount[i]>maxNodeCount){
//                    maxNodeCount=nodeCategoryCount[i];
//                    maxNodeCategory = i;
//                }
//            }
//            Arrays.sort(nodeCategoryCount);
//            for(int i=0;i<nodeCategoryCount.length;i++){
//                if(nodeCategoryCount[i]>0){
//                    System.out.print(nodeCategoryCount[i]+",");
//                }
//            }
//            System.out.println();
//            System.out.println("max category:" + maxNodeCategory + " has nodes:" + maxNodeCount+" node(id) start:"+categoryStartNodes.get(maxNodeCategory).getId());
//            System.out.println("total nodes:"+totalNodes);
//            System.out.println("category count:" + connectedSubNetId[0]);
//            ===================== clear all =================
            Arrays.fill(nodeCategoryCount,0);
            categoryStartNodes.clear();
            connectedSubNetId[0]=0;
            visited.clear();

            new TransactionWrapper() {
                public void runInTransaction() {
                    for (Node node : GlobalGraphOperations.at(db).getAllNodes()) {
                        if (!visited.contains(node.getId())) {
                            final int[] nodeInOneCategory={0};
                            categoryStartNodes.put(connectedSubNetId[0], node);
                            traversal.BFS(node, visited, new TGraphTraversal.BFSAction<Node,Relationship>() {
                                public boolean visit(Node node) {
                                    nodeInOneCategory[0]++;
//                                    node.setProperty("connected-subnet-id", connectedSubNetId[0]);
                                    return true;
                                }
                            }, false);
//                            System.out.println("category:" + connectedSubNetId[0] + "has nodes "+nodeInOneCategory[0]+" start node id:"+node.getId());
                            nodeCategoryCount[connectedSubNetId[0]]=nodeInOneCategory[0];
                            connectedSubNetId[0]++;
                        }
                    }
                }
            }.start(db);
            maxNodeCount=0;
            maxNodeCategory=0;
            for(int i=0;i<nodeCategoryCount.length;i++){
                if(nodeCategoryCount[i]>maxNodeCount){
                    maxNodeCount=nodeCategoryCount[i];
                    maxNodeCategory = i;
                }
            }
            Arrays.sort(nodeCategoryCount);
            for(int i=0;i<nodeCategoryCount.length;i++){
                if(nodeCategoryCount[i]>0){
                    System.out.print(nodeCategoryCount[i]+",");
                }
            }
            System.out.println();
            System.out.println("max category:" + maxNodeCategory + " has nodes:" + maxNodeCount+" node(id) start:"+categoryStartNodes.get(maxNodeCategory).getId());
            System.out.println("total nodes:"+totalNodes);
            System.out.println("category count:"+connectedSubNetId[0]);
        } catch(RuntimeException e){
            e.printStackTrace();
        } catch (Exception e){
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally{
            if(db!=null)db.shutdown();
        }

        System.exit(0);
    }

    private void initDB(){
        db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(config.dbPath)
                .loadPropertiesFromFile("")
                .newGraphDatabase();
    }
}
