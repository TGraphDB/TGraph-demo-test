package org.act.tgraph.demo.benchmark.transaction;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.base.Preconditions;

import java.util.List;

public class ImportTemporalDataTx extends AbstractTransaction {
    public final StatusUpdate[] data;
    public ImportTemporalDataTx(List<StatusUpdate> lines) {
        super(TxType.tx_import_temporal_data);
        this.data = lines.toArray(new StatusUpdate[0]);
    }

    public ImportTemporalDataTx(JsonObject obj){
        super(TxType.tx_import_temporal_data);
        Preconditions.checkArgument( TxType.valueOf(obj.get("type").asString()) == TxType.tx_import_temporal_data );
        JsonArray idArr = obj.get("id").asArray();
        JsonArray timeArr = obj.get("time").asArray();
        JsonArray travelTimeArr = obj.get("travelTime").asArray();
        JsonArray jamStatusArr = obj.get("jamStatus").asArray();
        JsonArray segCntArr = obj.get("segCnt").asArray();
        data = new StatusUpdate[idArr.size()];
        for(int i=0; i<idArr.size(); i++){
            data[i] = new StatusUpdate(idArr.get(i).asLong(), timeArr.get(i).asInt(), travelTimeArr.get(i).asInt(), jamStatusArr.get(i).asInt(), segCntArr.get(i).asInt());
        }
    }

    @Override
    public String encode() {
        JsonObject obj = newTx(TxType.tx_import_temporal_data);
        JsonArray idArr = new JsonArray();
        JsonArray timeArr = new JsonArray();
        JsonArray travelTimeArr = new JsonArray();
        JsonArray jamStatusArr = new JsonArray();
        JsonArray segCntArr = new JsonArray();
        for(StatusUpdate s : data){
            idArr.add(s.roadId);
            timeArr.add(s.time);
            travelTimeArr.add(s.travelTime);
            jamStatusArr.add(s.jamStatus);
            segCntArr.add(s.segmentCount);
        }
        obj.add("id", idArr);
        obj.add("time", timeArr);
        obj.add("travelTime", travelTimeArr);
        obj.add("jamStatus", jamStatusArr);
        obj.add("segCnt", segCntArr);
        return obj.toString();
    }

    @Override
    public int dataCount() {
        return data.length;
    }

    public static class StatusUpdate{
        public final long roadId;
        public final int time;
        public final int travelTime;
        public final int jamStatus;
        public final int segmentCount;

        public StatusUpdate(long roadId, int time, int travelTime, int jamStatus, int segmentCount) {
            this.roadId = roadId;
            this.time = time;
            this.travelTime = travelTime;
            this.jamStatus = jamStatus;
            this.segmentCount = segmentCount;
        }
    }
}
