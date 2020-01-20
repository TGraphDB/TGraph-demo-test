package org.act.tgraph.demo.benchmark.client;

import com.eclipsesource.json.JsonObject;
import com.google.common.util.concurrent.ListenableFuture;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;

import java.io.IOException;

/**
 * Created by song on 16-2-23.
 */
public interface DBProxy {
    // return server name.
    String testServerClientCompatibility() throws UnsupportedOperationException;

    ListenableFuture<JsonObject> execute(AbstractTransaction tx ) throws Exception;

    void createDB() throws IOException;

    // restart database. blocking until service available (return true) or failed (return false).
    void restartDB() throws IOException;

    void shutdownDB() throws IOException;

    void close() throws IOException, InterruptedException;
}
