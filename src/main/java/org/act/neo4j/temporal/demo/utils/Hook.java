package org.act.neo4j.temporal.demo.utils;

/**
 * Created by song on 16-5-12.
 */
public interface Hook<T> {
    void handler(T value);
}
