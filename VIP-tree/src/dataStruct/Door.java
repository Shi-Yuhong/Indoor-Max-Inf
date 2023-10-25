package dataStruct;

import java.util.ArrayList;
import java.util.Objects;

// 本类用于描述 门
public class Door {

    // 坐标为负值：当前坐标无效
    private double x = -1; // x坐标
    private double y = -1; // y坐标
    // 所属的分区（一个门最多属于两个分区
    private Integer p1=-1;
    private Integer p2=-1;
    // 从属的叶子结点id
    // -1: 默认设置，表示无归属
    // -2: 表示为AD
    // 非负数: 归属的叶子结点id
    private int Belong = -1;

    public Door(Integer p1, Integer p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public Door(double x, double y, Integer p1, Integer p2) {
        this.x = x;
        this.y = y;
        this.p1 = p1;
        this.p2 = p2;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Integer getP1() {
        return p1;
    }

    public Integer getP2() {
        return p2;
    }

    public int getBelong() {
        return Belong;
    }

    public void setBelong(int belong) {
        Belong = belong;
    }

    public int getAdjacent(int p){
        // 相邻分区会在同一条D2P记录中
        if(p1==p)
            return p2;
        if(p2==p)
            return p1;
        // 表示出错
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Door door = (Door) o;
        return Double.compare(door.getX(), getX()) == 0 && Double.compare(door.getY(), getY()) == 0 && getBelong() == door.getBelong() && Objects.equals(getP1(), door.getP1()) && Objects.equals(getP2(), door.getP2());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY(), getP1(), getP2(), getBelong());
    }

    @Override
    public String toString() {
        return "Door{" +
                "x=" + x +
                ", y=" + y +
                ", p1=" + p1 +
                ", p2=" + p2 +
                '}';
    }
}
