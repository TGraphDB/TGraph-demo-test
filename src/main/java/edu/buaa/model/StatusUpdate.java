package org.act.tgraph.demo.model;

import org.act.tgraph.demo.utils.MultiFileReader;

import java.io.File;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * Created by song on 2019-12-26.
 */
public class StatusUpdate {
    public final int travelTime;
    public final byte jamStatus;
    public final byte segmentCount;
    public final String roadId;
    public final int time;

    /**
     * data line likes:
     * 05270005 595640_00033 1 1 18
     * Explain: month(05),day(27),hour(00),minute(05) grid(595640),chain(00033) jamStatus(1) segCount(1) travelTime(18)
     * @param line traffic data from one road
     */
    public StatusUpdate(String line) {
        String[] fields = line.split(" ");
        time = timeStr2timestamp(fields[0]);
        roadId = fields[1];
        jamStatus = Byte.parseByte(fields[2]);
        segmentCount = Byte.parseByte(fields[3]);
        travelTime = Integer.parseInt(fields[4]);
    }

    /**
     *
     * @param tStr 05270005: month(05),day(27),hour(00),minute(05)
     * @return timestamp by seconds
     */
    private int timeStr2timestamp(String tStr){
        String monthStr = tStr.substring(0,2);
        String dayStr = tStr.substring(2,4);
        String hourStr = tStr.substring(4,6);
        String minuteStr = tStr.substring(6, 8);
        int month = Integer.parseInt(monthStr)-1;//month count from 0 to 11, no 12
        int day = Integer.parseInt(dayStr);
        int hour = Integer.parseInt(hourStr);
        int minute = Integer.parseInt(minuteStr);
        Calendar ca= Calendar.getInstance();
        ca.set(2010, month, day, hour, minute, 0); //seconds set to 0
        long timestamp = ca.getTimeInMillis();
        if(timestamp/1000<Integer.MAX_VALUE){
            return (int) (timestamp/1000);
        }else {
            throw new RuntimeException("timestamp larger than Integer.MAX_VALUE, this should not happen");
        }
    }

}
