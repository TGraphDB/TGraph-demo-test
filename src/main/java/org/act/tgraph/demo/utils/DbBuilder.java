package org.act.tgraph.demo.utils;

import org.act.tgraph.demo.vo.Cross;
import org.act.tgraph.demo.vo.RelType;
import org.act.tgraph.demo.vo.RoadChain;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DbBuilder {

    public void importNetwork2db(String topoFilePath, String dbPath) throws IOException {

        List<RoadChain> roadChainList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(topoFilePath))) {
            String line;
            for (int lineCount = 0; (line = br.readLine()) != null; lineCount++) {
                if (lineCount == 0) continue;//ignore headers
                try {
                    roadChainList.add(new RoadChain(line, lineCount));
                }catch (RuntimeException e){
                    System.out.println(e.getMessage()+" at line:"+lineCount);
                }
            }
        }
        for(RoadChain roadChain: roadChainList){
            roadChain.updateNeighbors();
        }

        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbPath));
        try(Transaction tx = db.beginTx()) {
            for (RoadChain roadChain : roadChainList) {
                int inCount = roadChain.getInNum();
                int outCount = roadChain.getOutNum();
                if (inCount > 0 || outCount > 0) {
                    Cross inCross = Cross.getStartCross(roadChain);
                    Cross outCross = Cross.getEndCross(roadChain);
                    Node inNode, outNode;
                    if (inCross.getNode(db) == null) {
                        inNode = db.createNode();
                        inCross.setNode(inNode);
                        inNode.setProperty("cross_id", inCross.id);
                    } else {
                        inNode = inCross.getNode(db);
                    }
                    if (outCross.getNode(db) == null) {
                        outNode = db.createNode();
                        outCross.setNode(outNode);
                        outNode.setProperty("cross_id", outCross.id);
                    } else {
                        outNode = outCross.getNode(db);
                    }

                    Relationship r = inNode.createRelationshipTo(outNode, RelType.ROAD_TO);
                    r.setProperty("grid_id", roadChain.getGridId());
                    r.setProperty("chain_id", roadChain.getChainId());
//                    r.setProperty("uid", roadChain.getUid());
//                    r.setProperty("type", roadChain.getType());
//                    r.setProperty("length", roadChain.getLength());
//                    r.setProperty("angle", roadChain.getAngle());
//                    r.setProperty("in_count", roadChain.getInNum());
//                    r.setProperty("out_count", roadChain.getOutNum());
//                    r.setProperty("in_roads", roadChain.briefInChain());
//                    r.setProperty("out_roads", roadChain.briefOutChain());
//                    r.setProperty("data_count", 0);
//                    r.setProperty("min_time", Integer.MAX_VALUE);
//                    r.setProperty("max_time", 0);
                }
            }
            tx.success();
        }
        db.shutdown();
    }
}
