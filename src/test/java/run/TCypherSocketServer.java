package run;

import org.act.tgraph.demo.utils.TGraphSocketServer;
import org.act.tgraph.demo.vo.RuntimeEnv;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;

public class TCypherSocketServer {
    public static void main(String[] args){
        RuntimeEnv env = RuntimeEnv.getCurrentEnv();
        String serverCodeVersion = env.name() + "." + env.getConf().codeGitVersion();
        TGraphSocketServer server = new TGraphSocketServer(env.getConf().dbPath, serverCodeVersion, new TCypherReqExecutor());
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class TCypherReqExecutor extends TGraphSocketServer.ReqExecutor{

        @Override
        protected String execute(String line) throws RuntimeException {
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
