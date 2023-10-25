package dataStruct;

import java.util.*;

// 该类定义了 IP-tree 中的节点
public class Node {

    public static double maxDist = 1000000; // 最大距离

    // IP-tree 成员变量
    protected int id; // 本节点编号
    protected int Pid; // 父节点编号
    // Set好处在于不用担心重复添加（而且本身内容有序性并不重要）
    protected HashSet<Integer> Cid; // 子节点&包含分区编号
    protected HashSet<Integer> AD; // 节点对应的AccessDoor编号
    protected HashMap<Integer,HashMap<Integer,MItem>> DM; //距离矩阵
    protected HashMap<Integer,Integer> Adjacent; // 邻居
    protected Boolean isConnect; // 是否是联通的节点 （测试文件中存在局部不联通的例子
    protected HashSet<Integer> Partition; // 包含的底层分区
    protected double radius; // 节点直径


    public Node() {
        Pid = -1; // 默认没有父节点
        Cid = new HashSet<>();
        AD = new HashSet<>();
        DM = new HashMap<>();
        Adjacent = new HashMap<>();
        Partition = new HashSet<>();
        isConnect = true; // 默认情况下联通
        radius = maxDist;

    }

    public void setId(int id) {
        this.id = id;
    }

    public void addCid(int id){
        Cid.add(id);
    }

    public int getId() {
        return id;
    }

    public int getPid() {
        return Pid;
    }

    public HashSet<Integer> getCid() {
        return Cid;
    }

    public HashSet<Integer> getAD() {
        return AD;
    }

    public HashMap<Integer, HashMap<Integer, MItem>> getDM() {
        return DM;
    }

    public HashMap<Integer, Integer> getAdjacent() {
        return Adjacent;
    }

    public void setPid(int pid) {
        Pid = pid;
    }

    public void setCid(HashSet<Integer> cid) {
        Cid = cid;
    }

    public Boolean getConnect() {
        return isConnect;
    }

    public void setConnect(Boolean connect) {
        isConnect = connect;
    }

    public HashSet<Integer> getPartition() {
        return Partition;
    }

    public void setPartition(HashSet<Integer> partition) {
        Partition = partition;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getRadius() {
        return radius;
    }


    // 添加子节点分区集合
    public void addPartiton(HashSet<Integer> add){
        Partition.addAll(add);
    }

    // 判定AD
    public void judgeAD(Map<Integer, Node> Tree){
        // 遍历所有子节点的AD，并统计出现次数
        // 从中选出仅出现1次的作为父节点的AD
        HashMap<Integer,Integer> temp = new HashMap<>();
        for(int i:Cid){
            HashSet<Integer> ad = Tree.get(i).getAD();
            for(int j:ad){
                if(!temp.containsKey(j)){
                    temp.put(j,1);
                }
                else{
                    int newdata = temp.get(j)+1;
                    temp.replace(j,newdata);
                }
            }
        }
        for(int i: temp.keySet()){
            if(temp.get(i)==1){
                AD.add(i);
            }
        }
    }

    // 计算邻居
    public void calAdjacent(HashMap<Integer,Integer> Adjacent,ArrayList<Integer> choose,int start){
        // 只需要替换掉节点序号就可以了
        for(int i:Adjacent.keySet()){
            int index = choose.indexOf(i) + start;
            this.Adjacent.put(index,Adjacent.get(i));
        }
        // 没有邻居节点，即为不连通
        if(this.Adjacent.size()==0){
            this.isConnect = false;
        }
    }

    // 计算节点距离矩阵
    public void NodeDM(Map<Integer,Node> Tree,HashMap<Integer, HashMap<Integer, MItem>> ADM,HashSet<Integer> NodeAD,HashMap<Integer,HashSet<Integer>> ADrecord){
        // 非叶子结点的DM建立是基于ADM求解的
        // 具体思路如下：ADM保存了AD-AD之间的最短距离和下一条信息，通过溯源的方法，可求出完整路径
        // 但索引结构只要求存储第一跳信息，第一跳是本层节点的AD

        // 1. 求解子节点包含的所有AD
        HashSet<Integer> MAD =new HashSet<>();
        for(int i:Cid){
            MAD.addAll(Tree.get(i).getAD());
        }
        // 2. 创建DM
        for(int i:MAD){
            HashMap<Integer,MItem> map = new HashMap<>();
            DM.put(i,map);
            ADrecord.get(i).add(id);
            for(int j:MAD){
                MItem mItem = new MItem(maxDist);
                if(i==j){
                    mItem.setDistance(0);
                }
                else{
                    mItem.setDistance(ADM.get(i).get(j).getDistance());
                    int hoop = ADM.get(i).get(j).getHoop();
                    while(hoop!=-1 && !NodeAD.contains(hoop)){
                        hoop = ADM.get(i).get(hoop).getHoop();
                    }
                    mItem.setHoop(hoop);
                }
                map.put(j,mItem);
            }
        }

        // 打印验证
        /*System.out.println("distance matrix: ");
        for(int i:DM.keySet()){
            HashMap<Integer,MItem> map = DM.get(i);
            for(int j:map.keySet()){
                if(map.get(j).getDistance()<maxDist){
                    System.out.println(i+" - "+j+" : "+map.get(j).getDistance()+" "+map.get(j).getHoop());
                }
            }
        }*/

    }

    // 计算节点半径
    public void calRadius(HashMap<Integer,Node> TreeNode){
        radius = 0;
        for(int i:Cid){
            radius = radius+ TreeNode.get(i).getRadius();
        }
    }

}
