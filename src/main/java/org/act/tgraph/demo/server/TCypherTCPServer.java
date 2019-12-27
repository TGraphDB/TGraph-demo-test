package org.act.tgraph.demo.server;

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
        protected String execute(String line) throws RuntimeException {
            if(line.startsWith("TOPIC:")){
                String testTopic = line.substring(6);
                System.out.println("topic changed to "+ testTopic);
                RuntimeEnv env = RuntimeEnv.getCurrentEnv();
                String serverCodeVersion = env.name() + "." + env.getConf().codeGitVersion();
                System.out.println("server code version: "+ serverCodeVersion);
                return "Server code version:"+serverCodeVersion;
            }else if("ID MAP".equals(line)){
                System.out.println("building id map...");
                String idMap = buildRoadIDMap();
                System.out.println("done. size:"+idMap.length()+" sending...");
                return idMap;
            }
            String[] queries = line.split(";");
            StringBuilder results = new StringBuilder();
            int i=0;
            try {
                try (Transaction tx = db.beginTx()) {
                    for (i = 0; i < queries.length; i++) {
                        String query = queries[i];
                        Result result = db.execute(query);
                        results.append( result.resultAsString().replace("\n", "\\n").replace("\r", "\\r") );
                    }
                    tx.success();
                }
            }catch (Exception msg){
//                StringWriter errors = new StringWriter();
//                msg.printStackTrace(new PrintWriter(errors));
//                results[i] = errors.toString();
                msg.printStackTrace();
                throw new TGraphSocketServer.TransactionFailedException();
            }
            return results.toString();
        }
    }
}
