package dataStruct;

import java.util.HashSet;

// 该类后续用于 全局ADM计算, 局部最短AD对距离及所属节点id !!!
public class AD2ADItem {

    // 对应AD-AD的距离
    private double distance;
    // 包含对应AD2AD记录的叶子节点id集合
    private HashSet<Integer> belong= new HashSet<>();

    public AD2ADItem(double distance, int belong) {
        this.distance = distance;
        this.belong.add(belong);
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public HashSet<Integer> getBelong() {
        return belong;
    }

    public void addBelong(int add){
        belong.add(add);
    }

    public void changeBelong(int change){
        // 易主：清空后重新添加
        belong.clear();
        belong.add(change);
    }
}