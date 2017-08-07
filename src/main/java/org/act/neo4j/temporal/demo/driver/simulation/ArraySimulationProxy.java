package org.act.neo4j.temporal.demo.driver.simulation;

import org.act.neo4j.temporal.demo.driver.OperationProxy;
import org.neo4j.graphdb.PropertyContainer;

/**
 * Created by song on 16-2-27.
 */
public class ArraySimulationProxy implements OperationProxy{

    @Override
    public Object getAggregate(PropertyContainer container, String key, int from, int to, Aggregator aggregator) {
        //TODO:need an exact definition of this operation's semantic.
        throw new RuntimeException("feature not completed yet.");
//        Integer[] value = (Integer[]) container.getProperty(key, null);
//        if (value != null) {
//            int len = value.length;
//            for(int i=0;i<len;i+=2){
//                if(value[i]<=to && value[i]>from){
//                    aggregator.add(value[i],value[i+1]);
//                }
//            }
//        }
//        return null;
    }

    @Override
    public Object get(PropertyContainer container, String key, int time) {
        int[] value = (int[]) container.getProperty(key, null);
        if (value != null) {
            int len = value.length;
            for(int i=0;i<len;i+=2){
                if(value[i]<=time){
                    return value[i+1];
                }
            }
        }
        return null;
    }

    @Override
    public void set(PropertyContainer pContainer, String key, int time, Object content) {
        synchronized (pContainer) {
            int[] value = (int[]) pContainer.getProperty(key, null);
            if (value == null) {
                pContainer.setProperty(key,new int[]{time,(Integer) content});
                return;
            } else {
                int len = value.length;
                for(int i=0;i<len;i+=2){
                    if(value[i]==time){
                        value[i+1]= (Integer) content;
                        pContainer.setProperty(key,value);
                        return;
                    }else if(value[i]>time){
                        continue;
                    }else{ // value[i]<time
                        int[] newValue = new int[len+2];
                        newValue[i]=time;
                        newValue[i+1]=(Integer)content;
                        System.arraycopy(value,0,newValue,0,i);
                        System.arraycopy(value,i,newValue,i+2,len-i);
                        pContainer.setProperty(key, newValue);
                        return;
                    }
                }
                // smaller than least key.
                int[] newValue = new int[len+2];
                newValue[len-2]=time;
                newValue[len-1]=(Integer)content;
                System.arraycopy(value,0,newValue,0,len);
                pContainer.setProperty(key,newValue);
                return;
            }
        }
    }

    public void initImport(PropertyContainer container, String key, int len) {
        int[] value = new int[len*2];
        for(int i=0;i<value.length;i+=2){
            value[i]=len-i;
            value[i+1]=len-i;
        }
        container.setProperty(key,value);
    }
}
