package dataStruct;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class NodeLeaf extends Node {
    // 节点内包含的Door编号：普通门&通道门
    private HashSet<Integer> D;
    private HashMap<Integer,HashSet<Integer>> SD; // 高级门

    public NodeLeaf() {
        super();
        D = new HashSet<>();
        SD = new HashMap<>();
    }

    public HashSet<Integer> getD() {
        return D;
    }

    public HashMap<Integer, HashSet<Integer>> getSD() {
        return SD;
    }

    // 添加分区
    public void addP(int i, HashMap<Integer,Partition> P) {
        // 添加分区编号&门信息
        Cid.add(i);
        HashSet<Integer> idoor = P.get(i).getD();
        D.addAll(idoor);
    }

    // 判定AD
    public void judgeAD(HashMap<Integer,Door> Doors, ArrayList<Integer> Outdoors,HashMap<Integer,HashSet<Integer>> ADrecord) {
        // 判定AD
        for (int i : D) {
            int p1 = Doors.get(i).getP1();
            int p2 = Doors.get(i).getP2();
            // 信息有误
            if(p1==-1&&p2==-1){
                System.out.println("平面图门信息有误或者程序读入相关信息出错！");
            }
            // 存在有一个为-1，通过Outdoors区分 外界门&内部门
            else if(p1==-1||p2==-1){
                // 外界门一定是AD
                if(Outdoors.contains(i)){
                    AD.add(i);
                    // ADrecord没有相关记录，新建记录
                    if(!ADrecord.containsKey(i)){
                        HashSet<Integer> ADrecordItem = new HashSet<>() ;
                        ADrecord.put(i,ADrecordItem);
                    }
                    // 添加id
                    ADrecord.get(i).add(id);
                }
            }
            // 门被两个分区所共有
            else{
                // 若有一个分区不属于本叶子节点，则该门为AD
                if(!Cid.contains(p1)||!Cid.contains(p2)){
                    AD.add(i);
                    // ADrecord没有相关记录，新建记录
                    if(!ADrecord.containsKey(i)){
                        HashSet<Integer> ADrecordItem = new HashSet<>() ;
                        ADrecord.put(i,ADrecordItem);
                    }
                    // 添加id
                    ADrecord.get(i).add(id);
                }
            }
        }
    }

    // 计算叶子节点邻居
    public void calAdjacent(HashMap<Integer,Door> Doors, int[] Pchoose) {
        // 遍历每一扇AD：AD居于边界，连接两个不同的节点
        for (int i : AD) {
            // 获取AD分界的两个分区序号
            int[] p = new int[2];
            p[0] = Doors.get(i).getP1();
            p[1] = Doors.get(i).getP2();
            for (int j : p) {
                // 外界，无需处理，可以跳出本层循环
                if (j == -1)
                    // 另外一个分区一定是节点包含的分区，并不是邻居，本AD没有添加项
                    break;
                else {
                    int n = Pchoose[j]; // 获取分区所在节点编号
                    if (n != id) {
                        // 不存在记录，新增
                        if (!Adjacent.containsKey(n)) {
                            Adjacent.put(n, 1);
                        }
                        // 存在记录，更新
                        else {
                            int temp = Adjacent.get(n) + 1;
                            Adjacent.replace(n, temp);
                        }
                        break; // 另一个就不用判断了
                    }
                }
            }
        }
        // 节点不连通的特殊情况
        if(this.Adjacent.size()==0){
            isConnect = false;
        }

    }

    // 计算AD2AD: 局部AD对最小距离统计
    public void getAD2AD(HashMap<Integer, HashMap<Integer, AD2ADItem>> AD2AD, HashMap<Integer, HashMap<Integer,HashSet<Integer>>> Affect) {
        // 遍历所有AD
        for (int i : AD) {
            // 获取计算对象
            HashMap<Integer, MItem> DMitem = DM.get(i);
            HashMap<Integer, AD2ADItem> AD2ADitem;
            // 之前已经有相关记录，直接取出
            if (AD2AD.containsKey(i)) {
                AD2ADitem = AD2AD.get(i);
            }
            // 之前没有相关记录，新建，并且加入到AD2AD中
            else {
                AD2ADitem = new HashMap<>();
                AD2AD.put(i, AD2ADitem);
            }
            // 遍历当前AD相关的DM记录
            for (int j : DMitem.keySet()) {
                // 另一个AD:易知该AD也应该属于本节点
                // 之后讨论的AD对为：AD_i→AD_j
                if (j != i && AD.contains(j)) {
                    // 在本节点内的距离
                    double my = DMitem.get(j).getDistance();
                    // 之前存在相关记录
                    if (AD2ADitem.containsKey(j)) {
                        // 在其他节点内的距离
                        double other = AD2ADitem.get(j).getDistance();
                        // 距离相等，只需要添加belong
                        if (other == my) {
                            AD2ADitem.get(j).addBelong(id);
                        }
                        // 否则距离不相等，有节点需要更新DM
                        // 当前距离更小
                        else if (other > my) {
                            // 更新Affect：原纪录所属的所有节点都需要更新
                            HashSet<Integer> changed = AD2ADitem.get(j).getBelong();
                            for(int changedID:changed){
                                AddAffect(Affect,changedID,i,j);
                            }
                            // 更新AD2AD记录：距离和所属
                            AD2ADitem.get(j).setDistance(my);
                            AD2ADitem.get(j).changeBelong(id);
                        }
                        // 原先的距离更近，现在的更远
                        // 当前对应的叶子结点受影响
                        else {
                            AddAffect(Affect,id,i,j);
                        }
                    }
                    // 之前不存在相关记录
                    else {
                        AD2ADItem adItem = new AD2ADItem(my, id);
                        AD2ADitem.put(j, adItem);
                    }
                }
            }
        }
    }

    // 计算叶子节点距离矩阵
    public void LeafDM(HashMap<Integer, HashMap<Integer, Double>> D2D) {
        // LeafDM：AD2D矩阵
        // 从每一个AD发起dijkstra
        for (int i : AD) {
            // 若无以下步骤，将发生空指针异常
            HashMap<Integer, MItem> map = new HashMap<>(); // 后续可以只对map操作：指针相同
            DM.put(i, map);
            // 初始化
            for (int j : D) {
                MItem mItem = new MItem(maxDist); // 对应i-j
                map.put(j, mItem);
                if (i == j) {
                    mItem.setDistance(0);
                }
                // D2D中包含i-j的记录
                if (D2D.containsKey(i) && D2D.get(i).containsKey(j)) {
                    mItem.setDistance(D2D.get(i).get(j));
                }
            }

            /*
            // 叶子节点DM初始化后验证
            for(int j: map.keySet()){
                System.out.println(i + "--"+j+": "+map.get(j).getDistance()+"\n");
            }
            System.out.println("\n\n");
             */

            // 开始dijkstra迭代计算
            int index = -2;
            // 选出未选择的距离最近的门序号
            HashSet<Integer> choose = new HashSet<>(); // 选择标记
            choose.add(i); // 防止选到自己
            while ((index = nextDoor(map, choose)) != -1) {
                // index=-1时，表示选择完毕
                choose.add(index);
                HashMap<Integer, Double> indexD = D2D.get(index);
                // 遍历与index相关的D2D记录
                for (int k : indexD.keySet()) {
                    // k同时包含于D，且之前未选过
                    if (D.contains(k) && !choose.contains(k)) {
                        // 合适地设置maxDist，防止越界
                        if (map.get(index).getDistance()+indexD.get(k) < map.get(k).getDistance()) {
                            // 更新记录
                            map.get(k).setDistance(map.get(index).getDistance() + indexD.get(k)); //本身其实也有越界风险
                            map.get(k).setHoop(index);
                        }
                    }
                }
            }

        }

        // 叶子节点DM构建后验证
        /*System.out.println("distance matrix: ");

        for(int i:AD){
            HashMap<Integer,MItem> map = DM.get(i);
            for(int j:map.keySet()){
                if(map.get(j).getDistance()==maxDist){
                    System.out.println(i+" - "+j+" :不可联通，可能有误");
                }else{
                    System.out.println(i+" - "+j+" : "+map.get(j).getDistance()+" "+map.get(j).getHoop());
                }
            }
        }
        System.out.println();*/

    }

    // 受影响信息：叶子节点id: AD_i→{AD_j}（受影响的AD门对距离）
    public void UpdataLeafDM(HashMap<Integer, HashMap<Integer, Double>> D2D,HashMap<Integer, HashMap<Integer, MItem>> ADM,HashMap<Integer, HashMap<Integer,HashSet<Integer>>> Affect){
        // 老实说这部分代码可以和上边的稍微合并一下的，因为大体相同，只是初始化部分不太一样
        // 好吧可能不太能合并，dijkstra里边还是有一些细节是不一样的
        // 获取本叶子节点的改变信息：AD_i→{AD_j}
        HashMap<Integer,HashSet<Integer>> affect = Affect.get(id);
        // 只要有边的权重发生变化，dijkstra就必须重新计算，不存在只改变局部的捷径
        DM.clear(); // 清空记录
        for(int i:AD){
            // 从AD_i开始计算DM
            HashSet<Integer> choose = new HashSet<>();
            choose.add(i);
            HashMap<Integer, MItem> map = new HashMap<>(); // 后续可以只对map操作：指针相同
            DM.put(i, map);
            HashSet<Integer> affectItem = null; // 后续应当有判空处理
            // 存在AD_i为起点的AD门对距离更新
            if(affect.containsKey(i)){
                affectItem = affect.get(i);
            }
            // 初始化操作
            for(int j:D){
                MItem mItem = new MItem(maxDist);
                map.put(j, mItem);
                // 正常赋值
                if (i == j) {
                    mItem.setDistance(0);
                }
                if (D2D.containsKey(i) && D2D.get(i).containsKey(j)) {
                    mItem.setDistance(D2D.get(i).get(j));
                }
                // 如果涉及更新的记录，则需要设置distance&hoop
                if(affectItem!=null && affectItem.contains(j)){
                    mItem.setDistance(ADM.get(i).get(j).getDistance());
                    // 存在更新说明对于当前的叶子节点，AD对之间的最短路径过节点外部，根据论文p7，此时Hoop为路径上第一扇AD
                    // 故而对应的Hoop设置应该没问题（吧
                    // 2023/10/20 修复: 这个设置有问题，直接getHoop是i-j的最后一跳而不是第一跳
                    // mItem.setHoop(ADM.get(i).get(j).getHoop());
                    int hoop = ADM.get(i).get(j).getHoop();
                    mItem.setHoop(hoop);
                    // System.out.println("update: "+ hoop+" "+AD.contains(hoop));

                }
            }

            // 开始dijkstra迭代计算
            int index = -2;
            while ((index = nextDoor(map, choose)) != -1) {
                choose.add(index);
                // index 对应计算值
                HashMap<Integer, Double> indexD = D2D.get(index);
                // 如果index是涉及更新的AD，则需要带入已经更新的距离值进行计算
                // 2023/10/21: 实际上应该不会发生这种可能  ====不知道为什么相关代码会被触发，但是最后验证说明是正确的
                //             假定发生，对应情况是 第三轮中，在第二轮最短距离计算结果上，有AD_i→AD_j→AD_k ≤ AD_i→AD_z
                //             然而第二轮的dijkstra已经排除了这种情况
                //             仍然保留下列代码，供后续数据集验证
                HashSet<Integer> indexItem = null; // 后续也应当有判空处理
                if(affect.containsKey(index)){
                    indexItem = affect.get(index);
                }

                for(int k:map.keySet()){
                    if(!choose.contains(k)){
                        // 涉及的index-k是包含更新的边，即此时 i/index/k都是AD
                        if(indexItem!=null && indexItem.contains(k) && map.get(index).getDistance()+ADM.get(index).get(k).getDistance()<map.get(k).getDistance()){
                            // System.out.println(index+"-"+k+": 三轮有限更新理论有误，算法不正确 // 原图局部视角，该边不连通");
                            map.get(k).setDistance(map.get(index).getDistance() + ADM.get(index).get(k).getDistance()); //本身其实也有越界风险
                            map.get(k).setHoop(index);
                            // 二轮更新正确性
                            if(indexD.containsKey(k) && indexD.get(k)<ADM.get(index).get(k).getDistance()){
                                System.out.println("代码有误，二轮更新结果一定不大于第一轮");
                            }
                        }
                        else if(indexD.containsKey(k) && map.get(index).getDistance() + indexD.get(k)< map.get(k).getDistance() ) {
                            map.get(k).setDistance(map.get(index).getDistance() + indexD.get(k)); //本身其实也有越界风险
                            map.get(k).setHoop(index);
                        }

                    }
//                    if(!choose.contains(k)&& indexItem!=null&& indexItem.contains(k)){
//                        if(map.get(index).getDistance()<map.get(k).getDistance()-ADM.get(index).get(k).getDistance()){
//                            map.get(k).setDistance(map.get(index).getDistance()+ADM.get(index).get(k).getDistance());
//                            map.get(k).setHoop(index);
//                        }
//                    }
//                    else if(!choose.contains(k)&&indexD!=null&&indexD.containsKey(k)){
//                        if (map.get(index).getDistance() < map.get(k).getDistance() - indexD.get(k)) {
//                            map.get(k).setDistance(map.get(index).getDistance() + indexD.get(k)); //本身其实也有越界风险
//                            map.get(k).setHoop(index);
//                        }
//                    }
                }
            }

            // 三轮有限更新正确性
            // 如果最后验证发现AD对距离被更新，那么就说明发生错误，将引发下一轮更新
            for(int ad:AD){
                if(map.get(ad).getDistance()<ADM.get(i).get(ad).getDistance()){
                    System.out.println("三轮有限更新理论有误，实际更新不止三轮");
                }
            }
        }
    }

    // 返回未选择的距离最近的门的序号
    public int nextDoor(HashMap<Integer, MItem> map, HashSet<Integer> choose) {

        int index = -1;
        double min = maxDist;
        // 遍历
        for (int i : map.keySet()) {
            // 自动忽略已选择的门
            if (!choose.contains(i) && map.get(i).getDistance() < min) {
                index = i;
                min = map.get(i).getDistance();
            }
        }
        // index
        return index;
    }

    // 获取待更新记录
    public static void AddAffect(HashMap<Integer, HashMap<Integer,HashSet<Integer>>> Affect,int changedID,int i,int j){
        HashMap<Integer,HashSet<Integer>> affect;
        // 事先有相关叶子节点的记录，直接读取，否则创建新的并添加
        if (Affect.containsKey(changedID)) {
            affect = Affect.get(changedID);
        } else {
            affect = new HashMap<>();
            Affect.put(changedID, affect);
        }
        HashSet<Integer> affectItem;
        if(affect.containsKey(i)){
            affectItem = affect.get(i);
        }
        else {
            affectItem = new HashSet<>();
            affect.put(i,affectItem);
        }
        affectItem.add(j);
    }

    // 重置SD
    public void resetSD(HashMap<Integer,Partition> Partitions){
        SD.clear();
        judgeSD(Partitions);
        /*System.out.println("====resetSD====");
        System.out.println("Node: "+id);
        System.out.println("AD: "+AD);
        System.out.println("SD: "+SD);*/
    }

    // 高级门的计算
    public void judgeSD(HashMap<Integer,Partition> Partitions){
        // 遍历每一个分区
        // System.out.println("\nsuper door:");
        for(int i:Partition){
            HashSet<Integer> pDoor = Partitions.get(i).getD(); // 分区对应的门
            HashSet<Integer> pSuperDoor = new HashSet<>(); // 分区对应的SD
            // 求取 localDoor 和 globalDoor
            // retainAll和removeAll都需要复制一遍集合，因为该操作会改变集合中的元素而非返回一个新的集合
            HashSet<Integer> localDoor = new HashSet<>(AD);
            localDoor.retainAll(pDoor);
            HashSet<Integer> globalDoor = new HashSet<>(AD);
            globalDoor.removeAll(localDoor);
            // SD界定：d∈P Ⅰ) localDoor
            //            Ⅱ) ョd' ∈ globalDoor,d→d'不经过P中其他门
            for(int j:pDoor){
                // 局部门即为SD
                if(localDoor.contains(j)){
                    pSuperDoor.add(j);
                }
                // 检查其他全局门
                // DM中存储的hoop是通过AD→D计算而来的，实际上是D→AD的第一扇门
                // 直接检查第一跳是否∈P即可
                else{
                    for(int k:globalDoor){
                        int hoop = DM.get(k).get(j).getHoop();
                        // System.out.println(k+" "+j+" "+hoop+"  "+DM.get(k).get(j).getDistance());
                        // 第一跳不经过P其他门：直通 / Pdoor不包含该跳
                        if(hoop==-1||!pDoor.contains(hoop)){
                            pSuperDoor.add(j);
                            // System.out.println("**** "+j+" - "+k+": "+hoop);
                            break; // 可确认当前门为SD，可以结束循环
                        }
                    }
                }
            }
            /*System.out.println(i+": "+pSuperDoor);
            System.out.println("Pdoor: "+pDoor);
            System.out.println("localDoor: "+localDoor);
            System.out.println("globalDoor: "+globalDoor);
            System.out.println();*/
            SD.put(i,pSuperDoor);
        }

    }

    // 标记门的归属
    public void setBelong(HashMap<Integer,Door> Doors){
        for(int i:D){
            if(AD.contains(i)){
                Doors.get(i).setBelong(-2);
            }
            else{
                Doors.get(i).setBelong(id);
            }
        }
    }

    // 计算叶子半径
    public void calLeafRadius(HashMap<Integer,Partition> Partitions){
        radius = 0;
        for(int i:Cid){
            radius = radius+ Partitions.get(i).getRadius();
        }
    }

}
