package edu.buaa.vo;

import edu.buaa.client.vo.RoadChain;

/**
 * Created by song on 16-2-26.
 */
public class Line {
    public int time;
    public String content;
    public RoadChain roadChain;
    public int value;
    public boolean exit;

    public Line(int time,RoadChain road,Integer value){
        this.time = time;
        this.roadChain = road;
        this.value = value;
    }

    public Line(int time,String content){
        this.time = time;
        this.content = content;
    }
}
