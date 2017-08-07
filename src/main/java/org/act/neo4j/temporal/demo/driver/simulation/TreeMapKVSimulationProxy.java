package org.act.neo4j.temporal.demo.driver.simulation;

import org.act.neo4j.temporal.demo.driver.OperationProxy;
import org.act.neo4j.temporal.demo.utils.Helper;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;

import java.io.*;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Base64;

/**
 * Created by song on 16-2-26.
 */
public class TreeMapKVSimulationProxy implements OperationProxy {
    @Override
    public String getAggregate(PropertyContainer container, String key, int from, int to, Aggregator aggregator) {
        TreeMap<Integer,Integer> value = decode(container.getProperty(key));
        if(value==null){
            return null;
        }else{
            NavigableMap<Integer, Integer> submap = value.subMap(from, true, to, true);
            if(submap==null){
                return null;
            }else{
                for(Map.Entry<Integer,Integer> v : submap.entrySet()){
                    aggregator.add(v.getKey(),v.getValue());
                }
                return aggregator.result();
            }
        }
    }

    @Override
    public String get(PropertyContainer container, String key, int time) {
        TreeMap<Integer,Integer> value = decode(container.getProperty(key));
        if(value==null){
            return null;
        }else{
            Map.Entry<Integer,Integer> v = value.floorEntry(time);
            if(v==null){
                return null;
            }else{
                return String.valueOf(v.getValue());
            }
        }
    }

    @Override
    public void set(PropertyContainer pContainer, String key, int time, Object content) {
        TreeMap<Integer, Integer> value;
        synchronized (pContainer) {
            try {
                value = decode(pContainer.getProperty(key));
            }catch (NotFoundException e){
                value = new TreeMap<Integer, Integer>();
            }
            value.put(time, (Integer) content);
//            System.out.println("encode:"+(encode(value)instanceof Byte[]));
            pContainer.setProperty(key, encode(value));
//            System.out.println(pContainer.getProperty(key)instanceof Byte[]);
        }
    }


    private TreeMap<Integer,Integer> decode(Object raw){
        try {
//            byte b[] = ((String)raw).getBytes();
            byte b[] = Base64.getDecoder().decode((String) raw);
//            System.out.println("coming here?");
//            System.out.println("decode result:"+Helper.bytesToHex(b));
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            return (TreeMap<Integer,Integer>) si.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String encode(TreeMap<Integer, Integer> obj){
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(obj);
            so.flush();
//            System.out.println("encode result:" + Helper.bytesToHex(bo.toByteArray()));
            return new String(Base64.getEncoder().encode(bo.toByteArray()));
//            return new String(bo.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
