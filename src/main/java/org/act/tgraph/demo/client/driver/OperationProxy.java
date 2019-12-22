package org.act.tgraph.demo.client.driver;

import org.act.tgraph.demo.client.driver.neo4j.Aggregator;
import org.neo4j.graphdb.PropertyContainer;

/**
 * Created by song on 16-2-23.
 */
public interface OperationProxy {

    Object getAggregate(PropertyContainer container, String key, int from, int to, Aggregator aggregator);

    Object get(PropertyContainer container, String key, int time) ;

    void set(PropertyContainer pContainer, String key, int time, Object content);

}
