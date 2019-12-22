package org.act.tgraph.demo.client.algo;

import org.act.tgraph.demo.client.utils.TransactionWrapper;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.*;


/**
 * Created by song on 16-5-8.
 */
public class TGraphTraversal {

    public static abstract class DFSAction<T>{
        public abstract boolean visit(T t);
    }

    public static abstract class BFSAction<T,K>{
        public boolean visit(T t){return false;}
        public boolean preVisit(T startNode, T endNode, K edge){return false;}
    }

    public static abstract class EdgeBFSAction{
        public boolean visit(Relationship t){return false;}
//        public boolean preVisit(T startNode, T endNode, K edge){return false;}
    }

    private boolean shouldGo=true;
    private GraphDatabaseService db;
    private long visitedNodeCount=0;

    public TGraphTraversal(GraphDatabaseService db){
        this.db = db;
    }

    public void cancel(){
        shouldGo=false;
    }

    public void BFS(final Node startNode, final Set<Long> isVisited,
                    final BFSAction<Node,Relationship> action, boolean wrapInTransaction){
        shouldGo=true;
        visitedNodeCount=0;
        if(wrapInTransaction){
            new TransactionWrapper(){
                public void runInTransaction() {
                    BFS(startNode, isVisited, action);
                }
            }.start(db);
        }else{
            BFS(startNode, isVisited, action);
        }
    }

    public void edgeBFS(final Relationship startEdge,
                    final EdgeBFSAction action, boolean wrapInTransaction){
        shouldGo=true;
        visitedNodeCount=0;
        if(wrapInTransaction){
            new TransactionWrapper(){
                public void runInTransaction() {
                    edgeBFS(startEdge, action);
                }
            }.start(db);
        }else{
            edgeBFS(startEdge, action);
        }
    }

    public void DFS(final Node startNode, final Set<Long> isVisited,
                    final DFSAction<Node> action, boolean wrapInTransaction){
        shouldGo=true;
        visitedNodeCount=0;
        if(wrapInTransaction){
            new TransactionWrapper(){
                public void runInTransaction() {
                    DFS(startNode, isVisited, action);
                }
            }.start(db);
        }else{
            DFS(startNode, isVisited, action);
        }
    }

    private void DFS(Node node, Set<Long> visitedNodes, DFSAction<Node> action){
        Node next, cur = node;
        Stack<Node> stack = new Stack<Node>();

        while(shouldGo){
            if(!visitedNodes.contains(cur.getId())){
                visitedNodeCount++;
                if(!action.visit(cur)) break;
                visitedNodes.add(cur.getId());
            }
            next = findNeighborNotVisited(cur, visitedNodes);
            if(next!=null){
                stack.push(cur);
                cur = next;
            }else{
                if(stack.empty()){ break; }
                else{
                    cur = stack.pop();
                }
            }
//            printStack(stack);
        }
    }

    private void BFS(Node startNode, Set<Long> visitedNodes, BFSAction<Node,Relationship> action){
        // init algorithm
        HashSet<Long> visitedEdges = new HashSet<Long>();
        visitedNodes.add(startNode.getId());
        Queue<Node> queue = new LinkedList<Node>();
        queue.add(startNode);

        // loop while queue not empty
        while (shouldGo && !queue.isEmpty()) {
            Node node = queue.poll();
            visitedNodeCount++;
            if(!action.visit(node)) return;
            for (Relationship r : node.getRelationships(Direction.INCOMING)) {
                Node fromNode = r.getOtherNode(node);
//                if(fromNode.getId()!=r.getStartNode().getId()) throw new RuntimeException("not from node!");
                if (!visitedNodes.contains(fromNode.getId())) {
                    visitedNodes.add(fromNode.getId());
                    queue.add(fromNode);
                    if (!action.preVisit(node, fromNode, r)) {
                        shouldGo = false;
                        return;
                    }
                }else{
                    if (!visitedEdges.contains(r.getId())) {
                        visitedEdges.add(r.getId());
                        if (!action.preVisit(node, fromNode, r)) {
                            shouldGo = false;
                            return;
                        }
                    }
                }
            }
            for (Relationship r : node.getRelationships(Direction.OUTGOING)) {
                Node neighbor = r.getOtherNode(node);
                if (!visitedNodes.contains(neighbor.getId())) {
                    visitedNodes.add(neighbor.getId());
                    queue.add(neighbor);
                    if (!action.preVisit(node, neighbor, r)) {
                        shouldGo = false;
                        return;
                    }
                }else{
                    if (!visitedEdges.contains(r.getId())) {
                        visitedEdges.add(r.getId());
                        if (!action.preVisit(node, neighbor, r)) {
                            shouldGo = false;
                            return;
                        }
                    }
                }
            }

        }
    }

    private void edgeBFS(Relationship startEdge, EdgeBFSAction action){
        // init algorithm
        HashSet<Long> visitedNodes = new HashSet<Long>();
        HashSet<Long> visitedEdges = new HashSet<Long>();
        visitedEdges.add(startEdge.getId());
        Queue<Relationship> queue = new LinkedList<Relationship>();
        queue.add(startEdge);

        // loop while queue not empty
        while (shouldGo && !queue.isEmpty()) {
            Relationship edge = queue.poll();
            if(!action.visit(edge)) return;

            nodeEdgeEnqueue(edge.getStartNode(), visitedEdges, visitedNodes, queue);
            nodeEdgeEnqueue(edge.getEndNode(), visitedEdges, visitedNodes, queue);
        }
    }

    private void nodeEdgeEnqueue(Node node, Set<Long> visitedEdges, Set<Long> visitedNodes, Queue<Relationship> queue){
        long nodeId = node.getId();
        if(!visitedNodes.contains(nodeId)) {
            for (Relationship r : node.getRelationships()) {
                long id = r.getId();
                if (!visitedEdges.contains(id)) {
                    visitedEdges.add(id);
                    queue.add(r);
                }
            }
            visitedNodes.add(nodeId);
        }
    }

    private Node findNeighborNotVisited(Node node, Set<Long> isVisited){
        for (Relationship r : node.getRelationships()) {
            Node neighbor = r.getOtherNode(node);
            if (!isVisited.contains(neighbor.getId())) {
                return neighbor;
            }
        }
        return null;
    }

    private void printStack(Stack<Node> stack){
        System.out.print("STACK[");
        for(Node node:stack){
            System.out.print(node.getId()+",");
        }
        System.out.println("]");
    }

    public long getVisitedNodeCount(){
        return visitedNodeCount;
    }
}
