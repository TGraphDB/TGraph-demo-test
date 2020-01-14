package org.act.tgraph.demo.client.driver;

import org.act.tgraph.demo.client.driver.neo4j.Aggregator;
import org.neo4j.graphdb.PropertyContainer;

/**
 * Created by song on 16-2-23.
 */
public interface DBOperationProxy {

    // restart database. blocking until service available (return true) or failed (return false).
    boolean restartDB();

    TxProxy beginTx();

    interface TxProxy{
        //
        long createNode(String crossId);

        long createRel(long startNode, long endNode, String name, int length, int type, int angle);

        long setTemporalProp(long roadId, int time, int jamStatus, int segCnt, int travelTime);

        int getJamStatus(long roadId, int time);

        Object getAggregate(PropertyContainer container, String key, int from, int to, Aggregator aggregator);

        Object get(PropertyContainer container, String key, int time) ;

        void set(PropertyContainer pContainer, String key, int time, Object content);

        void commit();
    }
}
