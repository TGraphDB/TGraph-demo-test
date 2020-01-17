package org.act.tgraph.demo.server;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.act.tgraph.demo.utils.TGraphSocketServer;
import org.act.tgraph.demo.client.vo.RuntimeEnv;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.IOException;

public class TCypherTCPServer {
    public static void main(String[] args){
        TGraphSocketServer server = new TGraphSocketServer(RuntimeEnv.getCurrentEnv().getConf().dbPath, new TCypherReqExecutor());
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class TCypherReqExecutor extends TGraphSocketServer.ReqExecutor{
        private String idMapStr;

        private String buildRoadIDMap() {
            if(idMapStr==null) {
                JsonObject map = new JsonObject();
                try (Transaction tx = db.beginTx()) {
                    for (Relationship r : GlobalGraphOperations.at(db).getAllRelationships()) {
                        String gridId = (String) r.getProperty("grid_id");
                        String chainId = (String) r.getProperty("chain_id");
                        String key = gridId + "," + chainId;
                        map.add(key, r.getId());
                    }
                    tx.success();
                }
                idMapStr = map.toString();
            }
            return idMapStr;
        }

        @Override
        protected JsonObject execute(String line) throws RuntimeException {
//            JsonObject req = Json.parse(line).asObject();
//            String[] queries = req.get("");
//            StringBuilder results = new StringBuilder();
//            int i=0;
//            try {
//                try (Transaction tx = db.beginTx()) {
//                    for (i = 0; i < queries.length; i++) {
//                        String query = queries[i];
//                        Result result = db.execute(query);
//                        results.append( result.resultAsString().replace("\n", "\\n").replace("\r", "\\r") );
//                    }
//                    tx.success();
//                }
//            }catch (Exception msg){
////                StringWriter errors = new StringWriter();
////                msg.printStackTrace(new PrintWriter(errors));
////                results[i] = errors.toString();
//                msg.printStackTrace();
//                throw new TGraphSocketServer.TransactionFailedException();
//            }
//            return results.toString();
            return new JsonObject();
        }
    }
}
