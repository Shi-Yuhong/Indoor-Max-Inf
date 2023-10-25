package dataStruct;

import java.util.*;

public class Partition {
    public static double maxDist = 1000000; // 最大距离
    private HashSet<Integer> D;
    private Boolean isHallway;
    private Boolean isConnect; // 是否是联通的分区（排除只进不出/不能进出的分区）
    // key-相邻分区  value-共同门数量
    private HashMap<Integer,Integer> Adjacent;
    private List<Map.Entry<Integer,Integer>> OrderAdjacent;
    private int Belong; // 从属的叶子结点id
    private double radius; // 分区半径（vita生成数据：分区最大对角线长度）

    public Partition() {
        D = new HashSet<>();
        isHallway = false;
        isConnect = true;
        Adjacent = new HashMap<>();
        Belong = -1; // 表示未分配至叶子结点
        radius = maxDist;
    }

    public HashSet<Integer> getD() {
        return D;
    }

    public void setHallway(Boolean hallway) {
        isHallway = hallway;
    }

    public Boolean getHallway() {
        return isHallway;
    }

    public HashMap<Integer, Integer> getAdjacent() {
        return Adjacent;
    }

    public Boolean getConnect() {
        return isConnect;
    }

    public void setConnect(Boolean connect) {
        isConnect = connect;
    }

    public int getBelong() {
        return Belong;
    }

    public void setBelong(int belong) {
        Belong = belong;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    // 往里添加新门
    public void addD(int d){
        D.add(d);
    }

    // 判定当前待加入的门是否已包含
    public boolean containD(int d){
        return D.contains(d);
    }

    // 更新Adjacent
    public void updateAdjacent(int p){
        // 重复出现，counter++
        if(Adjacent.containsKey(p)){
            int newValue = Adjacent.get(p)+1;
            Adjacent.replace(p,newValue);
        }
        // 否则新增记录
        else{
            Adjacent.put(p,1);
        }
    }

    public int getNumAdjacent(){
        return Adjacent.size();
    }

    public void setOrderAdjacent(){
        OrderAdjacent = new ArrayList<>(Adjacent.entrySet());
        OrderAdjacent.sort(new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());// 降序排序
            }
        });
        // System.out.println(OrderAdjacent);
    }

    public List<Map.Entry<Integer, Integer>> getOrderAdjacent() {
        return OrderAdjacent;
    }

    // 返回本次处理中具有最大共同门数量的未选择邻居
    public int getMaxAdjacent(int[] choose,int ban){
        // flag用于控制被ban的邻居选择
        int flag = 0; // 不可选状态
        // ban=-，百无禁忌
        if(ban==-1){
            flag = 1; // 可选状态
        }
        for(Map.Entry<Integer,Integer> item:OrderAdjacent){
            // ban以后的pid才是可选的
            int id = item.getKey();
            // 不可选状态下，判定是否到达被禁边界
            if(flag==0){
                if(id==ban){
                    flag=1; // 解禁
                }
                continue; // 跳过剩余操作
            }
            // 找到第一个 未被处理（choose[i]=-1）& 已有归处（choose[i]≥0）的邻居
            // 避开 本轮已选中（-2）&不可选（-3） 的
            if(choose[id] >=-1){
                return id;
            }
        }
        // 未找着，当前探索为死路
        return -1;
    }

    // 百无禁忌的选择具有最大共同门数量的未选择邻居
    public int getMaxAdjacent(int[] choose){
        for(Map.Entry<Integer,Integer> item:OrderAdjacent){
            int id = item.getKey();
            // 避开 本轮已选中（-2）&不可选（-3）
            if(choose[id] >=-1){
                return id;
            }
        }
        // 未找着
        return -1;
    }

}
