package dataStruct;

// 此类用于表示节点距离矩阵DM的条目
public class MItem {

    private double distance;
    private int hoop; // 中间跳节点id
    private double maxDist = Integer.MAX_VALUE;

    public MItem(double max) {
        maxDist = max;
        distance = maxDist; // 默认为最大值
        hoop = -1; // 表示没有下一跳门
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setHoop(int hoop) {
        this.hoop = hoop;
    }

    public double getDistance() {
        return distance;
    }

    public int getHoop() {
        return hoop;
    }
}


