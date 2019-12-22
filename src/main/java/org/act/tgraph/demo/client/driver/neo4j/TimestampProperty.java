package org.act.tgraph.demo.client.driver.neo4j;

/**
 * Created by song on 16-2-23.
 */
public class TimestampProperty {
    public final int time;
    public final Object value;
    public TimestampProperty(int time, Object value) {
        this.time = time;
        this.value = value;
    }
}
