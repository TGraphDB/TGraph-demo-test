package org.act.tgraph.demo.server;

import com.eclipsesource.json.JsonObject;
import org.act.tgraph.demo.client.utils.TGraphSocketServer;
import org.act.tgraph.demo.client.vo.RuntimeEnv;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.temporal.TimePoint;
import org.neo4j.tooling.GlobalGraphOperations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class KernelTCPServer {
    public static void main(String[] args){
        TGraphSocketServer server = new TGraphSocketServer(RuntimeEnv.getCurrentEnv().getConf().dbPath, new KernelWriteExecutor());
        RuntimeEnv env = RuntimeEnv.getCurrentEnv();
        String serverCodeVersion = env.name() + "." + env.getConf().codeGitVersion();
        System.out.println("server code version: "+ serverCodeVersion);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class KernelWriteExecutor extends TGraphSocketServer.ReqExecutor{
        private Map<Long, Relationship> idMap = new HashMap<>(140000);
        private String idMapStr;

        private String buildRoadIDMap() {
            if(idMapStr==null) {
                JsonObject map = new JsonObject();
                try (Transaction tx = db.beginTx()) {
                    for (Relationship r : GlobalGraphOperations.at(db).getAllRelationships()) {
                        String gridId = (String) r.getProperty("grid_id");
                        String chainId = (String) r.getProperty("chain_id");
                        String key = gridId + "," + chainId;
                        idMap.put(r.getId(), r);
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
                return "Server code version:"+serverCodeVersion;
            }else if("ID MAP".equals(line)){
                System.out.println("building id map...");
                String response = buildRoadIDMap();
                System.out.println("done. "+idMap.size()+" roads. sending...");
                return response;
            }
            String[] queries = line.split(";");
            try {
                try (Transaction tx = db.beginTx()) {
                    for (String query : queries) {
                        String[] arr = query.split(",");
                        long roadId = Long.parseLong(arr[0]);
                        int time = Integer.parseInt(arr[1]);
                        Relationship r = idMap.get(roadId);
                        r.setTemporalProperty("travel_time", TimePoint.unixMilli(time), Long.parseLong(arr[2]));
                        r.setTemporalProperty("full_status", TimePoint.unixMilli(time), Long.parseLong(arr[3]));
                        r.setTemporalProperty("vehicle_count", TimePoint.unixMilli(time), Long.parseLong(arr[4]));
                        r.setTemporalProperty("segment_count", TimePoint.unixMilli(time), Long.parseLong(arr[5]));
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
            return "";
        }
    }
}
