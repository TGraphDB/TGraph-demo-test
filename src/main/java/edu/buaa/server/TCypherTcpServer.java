package edu.buaa.server;

import edu.buaa.benchmark.client.TCypherExecutorClient;
import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.utils.TGraphSocketServer;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TCypherTcpServer extends TGraphSocketServer.ReqExecutor{
    public static void main(String[] args){
        TGraphSocketServer server = new TGraphSocketServer(dbDir(args), new TCypherTcpServer());
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File dbDir(String[] args){
        if(args.length<1){
            throw new IllegalArgumentException("need arg: dbDir");
        }
        File dbDir = new File(args[0]);
        if( !dbDir.exists() || !dbDir.isDirectory()){
            throw new IllegalArgumentException("invalid dbDir");
        }
        return dbDir;
    }

    @Override
    protected AbstractTransaction.Result execute(String line) throws RuntimeException {
        Map<String, List<Object>> results = new HashMap<>();
        try(Transaction tx = db.beginTx()){
            Result r = db.execute(line);
            while(r.hasNext()){
                Map<String, Object> row = r.next();
                row.forEach((k, v)->{
                    List<Object> list = results.computeIfAbsent(k, k1 -> new ArrayList<>());
                    list.add(v);
                });
            }
            tx.success();
        }
        TCypherExecutorClient.Result rr = new TCypherExecutorClient.Result();
        rr.setResults(results);
        return rr;
    }
}
