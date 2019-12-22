package org.act.tgraph.demo.client.driver.tgraph;

import org.act.temporalProperty.impl.InternalEntry;
import org.act.tgraph.demo.client.driver.OperationProxy;
import org.act.tgraph.demo.client.driver.neo4j.Aggregator;
import org.act.temporalProperty.util.Slice;

import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.temporal.TemporalRangeQuery;
import org.neo4j.temporal.TimePoint;

/**
 * Created by song on 16-2-23.
 */
public class Neo4jTemporalProxy implements OperationProxy
{
    @Override
    public String getAggregate(PropertyContainer container, String key, final int from, int to, Aggregator aggregator) {
        return (String) container.getTemporalProperty(key, new TimePoint(from), new TimePoint(to), new TemporalRangeQuery() {

            @Override
            public void setValueType(String valueType )
            {
                //
            }

            @Override
            public void onNewEntry( InternalEntry entry )
            {
                Slice value = entry.getValue();
                byte[] bytes = value.getBytes();
                System.out.println(bytes.length+":"+bytes);
                int i = value.getInt(0);
                System.out.println(i);
            }

            @Override
            public Object onReturn()
            {
                return null;
            }
        });

    }

    @Override
    public String get(PropertyContainer container, String key, int time) {
        try {
            return ""+container.getTemporalProperty(key, new TimePoint(time));
        }catch (NotFoundException e) {
            if (e.getMessage().contains("Dynamic property not exist!")) {
//                System.out.println("property["+key+"] not exist.ignore");
                //set(container,key,time,0);
            }
            return null;
        }catch (NullPointerException e){
            // skip;
            return null;
        }
    }

    @Override
    public void set(PropertyContainer pContainer, String key, int time, Object value) {
        try {
            pContainer.setTemporalProperty(key, new TimePoint(time), value);
        }catch (NotFoundException e) {

        }
    }
}
