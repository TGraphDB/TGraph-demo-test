package edu.buaa.server;

import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.utils.TGraphSocketServer;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.io.IOException;

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
        try(Transaction tx = db.beginTx()){
            db.execute(line);
            tx.success();
        }
        return null;
    }
}
