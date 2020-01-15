package org.act.tgraph.demo.benchmark;

import java.io.IOException;

/**
 * Created by song on 16-2-23.
 */
public interface DBOperation {

    void createDB() throws IOException;

    // restart database. blocking until service available (return true) or failed (return false).
    void restartDB() throws IOException;

    void shutdownDB() throws IOException;
}
