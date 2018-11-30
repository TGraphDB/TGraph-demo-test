package org.act.tgraph.demo.vo;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;

import java.util.*;

/**
 * Created by song on 16-1-22.
 */
public class RoadChain {
    private static Map<String,RoadChain> roadChainMap = new HashMap<>();
    private static int currentUid=1;



    public static RoadChain get(String gridId, String chainId){
        return roadChainMap.get(gridId+":"+chainId);
    }

    public static void setRoadChain(String gridId, String chainId, RoadChain roadChain) {
        if (roadChainMap.get(gridId + ":" + chainId) != null) {
            throw new RuntimeException("double road found!");
        }
        roadChain.setUid(currentUid);
        roadChainMap.put(gridId + ":" + chainId, roadChain);
        currentUid++;
    }

    public static String list2String(List<RoadChain> list){
        if(list.isEmpty()) return "[]";
        String result="";
        for(RoadChain roadChain: list){
            result+= ",r"+roadChain.getUid();
        }
        return "["+result.substring(1)+"]";
    }
    // an unique numeric id for each edge. This may vary depend on database status. for global unique id, see getUUID();
    private int uid; // usually equals lineNum-1, but NO confirm!;
    private String gridId;
    private String chainId;
    private int length;
    private int type;
    private int inNum = 0;
    private int outNum = 0;
    private int angle;
    private List<RoadChain> inChains = new ArrayList<>();
    private List<RoadChain> outChains = new ArrayList<>();

    private long relationshipId;

    public RoadChain(String line) { // it turns out both field[0] and field[3] are misleading.
        String[] fields = line.split(",");
//        index = Integer.valueOf(fields[0]);
        gridId = fields[1];
        chainId = fields[2];
//        int tmpIndex=Integer.valueOf(fields[3]);
//        if(tmpIndex!=index) throw new RuntimeException("index not equal in one line! Get "+index+" and "+tmpIndex);
//        index = Integer.valueOf(fields[3]);
        length = Integer.valueOf(fields[4]);
        type = Integer.valueOf(fields[5]);
        inNum = Integer.valueOf(fields[6]);
        outNum = Integer.valueOf(fields[7]);
        angle = Integer.valueOf(fields[8]);
        for(int i=0;i<inNum+outNum;i++){
            RoadChain neighbor = new RoadChain(fields[9+i*5],fields[10+i*5],fields[11+i*5],fields[12+i*5],fields[13+i*5]);
            if(i<inNum){
                inChains.add(neighbor);
            }else{
                outChains.add(neighbor);
            }
        }
        setRoadChain(gridId, chainId, this);
    }

    private RoadChain(String gridId, String chainId, String index, String length, String angle) {
        this.gridId = gridId;
        this.chainId = chainId;
//        this.index = Integer.parseInt(index);
        this.length = Integer.parseInt(length);
        this.angle = Integer.parseInt(angle);
    }

    public RoadChain(String line, int lineCount) {
        this(line);
//        this.lineNum = lineCount+1;
    }


    public String toString(){
        return "("+getUUID()+")," +
                "LEN"+length+",TYPE"+type+",ANGLE"+angle+",IN["+briefInChain()+"],OUT["+briefOutChain()+"]";
    }


    public String briefInChain(){
        return list2String(inChains);
    }

    public String briefOutChain(){
        return list2String(outChains);
    }

    private List<RoadChain> checkConstrain(List<RoadChain> roadList){
        List<RoadChain> newRoadList = new ArrayList<>();
        for(int i=0;i<roadList.size();i++) {
            RoadChain roadFromThisLine = roadList.get(i);
            RoadChain roadFromMap = get(roadFromThisLine.getGridId(), roadFromThisLine.getChainId());
            if (!((roadFromMap.gridId.equals(roadFromThisLine.gridId)) &&
                    (roadFromMap.chainId.equals(roadFromThisLine.chainId)) &&
//                    (roadFromMap.index == roadFromThisLine.index) &&
                    (roadFromMap.length == roadFromThisLine.length) &&
                    (roadFromMap.angle == roadFromThisLine.angle)
            )) {
                throw new RuntimeException("property not match! "+roadFromMap+roadFromThisLine);
            }else {
                newRoadList.add(roadFromMap);
            }
        }
        return newRoadList;
    }

    public void updateNeighbors() {
        inChains = checkConstrain(inChains);
        outChains = checkConstrain(outChains);
    }

    public int getOutNum() {
        return outNum;
    }

    public int getInNum() {
        return inNum;
    }

    public int getAngle() {
        return angle;
    }

    public int getType() {
        return type;
    }

    public int getLength() {
        return length;
    }

    public long getRelationshipId() {
        return relationshipId;
    }

    public void setRelationship(Relationship relationship) {
        this.relationshipId = relationship.getId();
    }

    public Relationship getRelationship(GraphDatabaseService db) {
        return db.getRelationshipById(this.relationshipId);
    }

    public String getGridId() {
        return gridId;
    }

    public String getChainId() {
        return chainId;
    }

    public String getUUID(){
        return gridId+":"+chainId;
    }

    public List<RoadChain> getInChains() {
        return inChains;
    }

    public List<RoadChain> getOutChains() {
        return outChains;
    }

    public static Comparator<RoadChain> comparator = new Comparator<RoadChain>() {
        @Override
        public int compare(RoadChain o1, RoadChain o2) {
            int uid1 = o1.getUid();
            int uid2 = o2.getUid();
            if (uid1 > uid2) {
                return 1;
            } else if (uid1 == uid2) {
                return 0;
            } else {
                return -1;
            }
        }
    };

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getUid() {
        return uid;
    }

//    public int getLineNum() {
//        return lineNum;
//    }
}
