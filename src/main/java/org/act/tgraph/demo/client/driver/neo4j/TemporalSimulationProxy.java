package org.act.tgraph.demo.client.driver.neo4j;

import org.act.tgraph.demo.client.driver.OperationProxy;

import org.neo4j.graphdb.PropertyContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by song on 16-2-3.
 */
public class TemporalSimulationProxy implements OperationProxy
{
    public void set(PropertyContainer propertyContainer, String name, int time, Object value){
        String key = name+":"+time;
        propertyContainer.setProperty(key, value);
    }

    public String get(PropertyContainer propertyContainer, String name, int time) {
        if (hasCharIn(name, ":")) {
            throw new RuntimeException("property name should not contains ':'");
        }
        List<Integer> timeList = new ArrayList<Integer>();
        String keyToGet = name + ":" + time;
        for (String key : propertyContainer.getPropertyKeys()) {
            if (keyToGet.equals(key)) {
                return new String((byte[])propertyContainer.getProperty(key));
            } else {
                if (key.startsWith(name)) {
                    timeList.add(Integer.valueOf(key.substring(name.length() + 1)));
                } else {
                    continue;
                }
            }
        }
        Integer[] keyListArray = timeList.toArray(new Integer[timeList.size()]);
        Arrays.sort(keyListArray, new Comparator<Integer>() {

            @Override
            public int compare(Integer o1, Integer o2) {
                if (o1 > o2) {
                    return 1;
                } else if (o1 == o2) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
        int pre = -1;
        for (Integer t : keyListArray) {

            if (t > time) {
                break;
            } else {
//                System.out.println(t);
                pre=t;
            }
        }

        if(pre==-1){
            throw new RuntimeException("no data at this time");
        }else {
            String key = name + ":" + pre;
//            System.out.println(key);
            return new String((byte[])propertyContainer.getProperty(key));
        }
    }

    public String getAggregate(
            PropertyContainer propertyContainer, String name, int from, int to, Aggregator aggregator){
        if (hasCharIn(name, ":")) {
            throw new RuntimeException("property name should not contains ':'");
        }
        for (String key : propertyContainer.getPropertyKeys()) {
            if (key.startsWith(name + ":")) {
                int time = Integer.valueOf(key.substring(name.length() + 1));
                if(time>=from && time<=to) {
                    boolean result = aggregator.add(time,propertyContainer.getProperty(key));
                    if(result==false){
                        return aggregator.result();
                    }
                }
            }
        }
        return aggregator.result();
    }

    public static List<TimestampProperty> getRangeList(PropertyContainer propertyContainer, String name, int from, int to){
        if (hasCharIn(name, ":")) {
            throw new RuntimeException("property name should not contains ':'");
        }
        List<TimestampProperty> timeList = new ArrayList<TimestampProperty>();
        for (String key : propertyContainer.getPropertyKeys()) {
            if (key.startsWith(name + ":")) {
                int time = Integer.valueOf(key.substring(name.length() + 1));
                if(time>=from && time<=to) {
                    timeList.add(new TimestampProperty(time, propertyContainer.getProperty(key)));
                }
            }
        }
        timeList.sort(new Comparator<TimestampProperty>() {
            @Override
            public int compare(TimestampProperty o1, TimestampProperty o2) {
                if (o1.time > o2.time) {
                    return 1;
                } else if (o1.time == o2.time) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });

        return timeList;
    }


    private static boolean hasCharIn(String string, String ch){
        int count = string.length() - string.replace(".", "").length();
        return count>0;
    }





}
