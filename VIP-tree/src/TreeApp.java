
import dataStruct.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TreeApp {

    // 系统参数
    private int NumHallway = 5; // 走廊判定
    private int NumChild = 2; // 分支数
    public static double maxDist = 1000000; // 最大距离
    // 相关文件路径
    public String path_D2P = "dataset/D2P.txt";
    public String path_D2D = "dataset/D2D.txt";
    public String path_P2D = "dataset/P2D.txt";

    // 相关数据结构
    private HashMap<Integer, HashMap<Integer, Double>> D2D; // D2D-Graph
    private HashMap<Integer, Door> Doors; // 门集
    private HashMap<Integer, Partition> Partitions; // 分区
    private HashMap<Integer, ArrayList<Integer>> NodeLevel; // 层次信息
    private HashMap<Integer, HashMap<Integer, MItem>> ADM; // 全局AD对距离矩阵
    private HashMap<Integer,HashSet<Integer>> ADrecord; // 定位AD的矩阵 key:AD_id  value:矩阵包含AD的节点


    // 主体-树节点
    private HashMap<Integer, Node> TreeNode;
    // 根节点记录 key:nodeId  value:level
    private ArrayList<Integer> RootNode;

    // IP-tree构建
    public TreeApp() {

        TreeNode = new HashMap<>();
        RootNode = new ArrayList<>();
        D2D = new HashMap<>();
        Doors = new HashMap<>();
        Partitions = new HashMap<>();
        NodeLevel = new HashMap<>();
        ADM = new HashMap<>();

        // 1. 创建D2D
        Read_D2D();

        // 2. 创建Doors
        Read_D2P();

        // 3. 创建Patitions
        ArrayList<Integer> Hallway = new ArrayList<>(); // 记录走廊分区的编号
        Map<Integer, Integer> PNumAdjacent = new HashMap<>(); // 记录分区与邻居数的对应关系
        // 2023/10/9: 引入Outdoors.用于区分外界门和分区内部门（否则但只从D2P的角度上来看，两者均只属于一个分区）
        ArrayList<Integer> Outdoors = new ArrayList<>(); // 记录通往建筑外界的门
        Read_P2D(Hallway, PNumAdjacent, Outdoors);

        // 4. 建立叶节点

        ADrecord = new HashMap<>();
        // CreatLeaf(PNumAdjacent, Hallway,Outdoors);
        CreatLeaf_Rev(PNumAdjacent, Hallway, Outdoors);

        // 5. 建立非叶子结点
        CreatNode();
        System.out.println("RootNode: " + RootNode);
        System.out.println("ADrecord: "+ADrecord);
        System.out.println("AD: "+ADrecord.keySet()+"\n\n");

    }

    // -----------------getter & setter-----------------
    public HashMap<Integer, HashMap<Integer, Double>> getD2D() {
        return D2D;
    }

    public void setD2D(HashMap<Integer, HashMap<Integer, Double>> d2D) {
        D2D = d2D;
    }

    public HashMap<Integer, Door> getDoors() {
        return Doors;
    }

    public void setDoors(HashMap<Integer, Door> doors) {
        Doors = doors;
    }

    public HashMap<Integer, Partition> getPartitions() {
        return Partitions;
    }

    public void setPartitions(HashMap<Integer, Partition> partitions) {
        Partitions = partitions;
    }

    public HashMap<Integer, ArrayList<Integer>> getNodeLevel() {
        return NodeLevel;
    }

    public void setNodeLevel(HashMap<Integer, ArrayList<Integer>> nodeLevel) {
        NodeLevel = nodeLevel;
    }

    public HashMap<Integer, HashMap<Integer, MItem>> getADM() {
        return ADM;
    }

    public void setADM(HashMap<Integer, HashMap<Integer, MItem>> ADM) {
        this.ADM = ADM;
    }

    public HashMap<Integer, Node> getTreeNode() {
        return TreeNode;
    }

    public void setTreeNode(HashMap<Integer, Node> treeNode) {
        TreeNode = treeNode;
    }

    public ArrayList<Integer> getRootNode() {
        return RootNode;
    }

    public void setRootNode(ArrayList<Integer> rootNode) {
        RootNode = rootNode;
    }

    // -----------------自定义函数-------------------

    // 读取D2D文件，创建D2D
    public void Read_D2D() {
        File file = new File(path_D2D);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                // 如果第一个字符是‘/’，则说明该行为注释，跳过本轮循环
                if (line.charAt(0) == '/')
                    continue;
                // 否则才是有效数据行
                String[] data = line.split(" ");
                // 文件中d的编号从1开始，但是正常程序中的编号从0开始
                int d1 = Integer.parseInt(data[0]) - 1;
                int d2 = Integer.parseInt(data[1]) - 1;
                double d3 = Double.parseDouble(data[2]);
                if (!D2D.containsKey(d1)) {
                    HashMap<Integer, Double> item1 = new HashMap<>();
                    D2D.put(d1, item1);
                }
                if (!D2D.containsKey(d2)) {
                    HashMap<Integer, Double> item2 = new HashMap<>();
                    D2D.put(d2, item2);
                }
                // 双向添加
                D2D.get(d1).put(d2, d3);
                D2D.get(d2).put(d1, d3);

            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    /*    System.out.println("---------D2D--------");
        for(int i: D2D.keySet()){
            HashMap<Integer,Double> temp = D2D.get(i);
            for(int j:temp.keySet()){
                System.out.println(i+"→"+j+": "+ temp.get(j));
            }
        }*/

    }

    // 读取D2P文件，创建Doors
    public void Read_D2P() {
        File file = new File(path_D2P);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                //如果第一个字符是‘/’，则说明该行为注释，跳过本轮循环
                if (line.charAt(0) == '/')
                    continue;
                //否则才是有效数据行
                String[] data = line.split(" ");

                // 文件中d的编号从1开始，程序中从0开始
                int Did = Integer.parseInt(data[0]) - 1;
                Door dItem = null;
                // 说明此时的数据包含有效坐标信息
                if (data.length > 3) {
                    // 使用正则表达式匹配坐标字符串模式
                    String pattern = "\\((-?\\d+.\\d+),(-?\\d+.\\d+)\\)";
                    Pattern regex = Pattern.compile(pattern);
                    Matcher matcher = regex.matcher(data[1]);
                    if (matcher.matches()) {
                        // 获取匹配的两个分组（double 值）
                        double x = Double.parseDouble(matcher.group(1));
                        double y = Double.parseDouble(matcher.group(2));
                        // 文件中p的编号从1开始，但是正常程序中的编号从0开始
                        dItem = new Door(x, y, Integer.parseInt(data[2]) - 1, Integer.parseInt(data[3]) - 1);
                    } else {
                        System.out.println("坐标格式不正确！");
                    }

                } else {
                    // 文件中p的编号从1开始，但是正常程序中的编号从0开始
                    // 当门连通外界时，对应的编号处理完为-1
                    dItem = new Door(Integer.parseInt(data[1]) - 1, Integer.parseInt(data[2]) - 1);
                }

                // System.out.println(Did+" "+dItem.toString());
                Doors.put(Did, dItem);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // 读取P2D文件，创建P2D
    public void Read_P2D(ArrayList<Integer> Hallway, Map<Integer, Integer> PNumAdjacent, ArrayList<Integer> Outdoors) {
        File file = new File(path_P2D);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                //如果第一个字符是‘/’，则说明该行为注释，跳过本轮循环
                if (line.charAt(0) == '/')
                    continue;
                //否则才是有效数据行
                String[] data = line.split(" ");
                // 文件中p的编号从1开始，但是正常程序中的编号从0开始
                int Pid = Integer.parseInt(data[0]) - 1;

                // 当前条目表示通往外界的门（这部分信息需要人工手动录入）
                if (Pid == -1) {
                    for (int i = 1; i < data.length; i++) {
                        Outdoors.add(Integer.parseInt(data[i]) - 1);
                    }
                    continue; // 跳过后续部分
                }

                Partition pItem = new Partition();
                // 如果门数达到Hallway，说明当前分区为走廊
                if (data.length > NumHallway) {
                    pItem.setHallway(true);
                    Hallway.add(Pid);
                }

                // 获取直径信息
                pItem.setRadius(Double.parseDouble(data[1]));

                // 遍历剩余data记录
                for (int i = 2; i < data.length; i++) {
                    int d = Integer.parseInt(data[i]) - 1;
                    // 文件中的D可能存在重复，如果不加判定可能会重复updateAdjacent
                    if (!pItem.containD(d)) {
                        pItem.addD(d);
                        // 获取包含当前门的另一分区（即相邻分区）
                        int ad = Doors.get(d).getAdjacent(Pid);
                        // 出错 / 与外界相连 / 当前门是内部门
                        if (ad != -1)
                            pItem.updateAdjacent(ad);
                    }
                }
                // 插入
                int adNum = pItem.getNumAdjacent();
                // 有邻居才算有意义，否则就是一个 只进不出/不连通 的分区，不会参与全局计算
                if (adNum > 0) {
                    pItem.setOrderAdjacent();
                    // System.out.println(Pid+" "+pItem.getOrderAdjacent());
                    // adNum = 0即无邻居分区不加入，否则后续生成叶子节点会出现空栈访问异常
                    PNumAdjacent.put(Pid, pItem.getNumAdjacent());
                } else {
                    pItem.setConnect(false);
                }
                // System.out.println(Pid+" "+pItem.getOrderAdjacent());
                Partitions.put(Pid, pItem);
            }
            reader.close();
            // System.out.println(Outdoors);
            // System.out.println(PNumAdjacent);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // 建立叶子节点
    public void CreatLeaf(Map<Integer, Integer> PNumAdjacent, ArrayList<Integer> Hallway, ArrayList<Integer> Outdoors) {

        // 由论文：num(Leaf) = num(Hallway)
        // 【2023/10/10】
        // re：更正！应该是 num(Leaf) ≥ num(Hallway)
        //      如果按照num(Leaf) = num(Hallway)，那么Hallway的判定得非常小心谨慎
        //      可能因为局部连通性差，使得为了分区可以被合并，持续调低Hallway判定值

        // 1. 创建Hallway个节点，每个节点放入一个走廊

        for (int i = 0; i < Hallway.size(); i++) {
            // 此时创建的都为叶子节点
            NodeLeaf nodeLeaf = new NodeLeaf();
            nodeLeaf.setId(i); // 节点id和在Tree中的key值保持相同
            nodeLeaf.addP(Hallway.get(i), Partitions);
            TreeNode.put(i, nodeLeaf);
        }

        // 2. 对PNumAdjacent按value（邻居数）升序排序
        List<Map.Entry<Integer, Integer>> PNumAd = new ArrayList<>(PNumAdjacent.entrySet());
        PNumAd.sort(new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            // NeedToCheck:如果value相同会不会被遗漏掉？(之前数据库实验有发生过
            // re:实际不会的，上述情况应该是发生在key/value是自定义类的情况
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        // System.out.println(PNumAd);

        // 3. 从最少的分区开始处理，每次合并与当前分区共同门数目最多的邻居（贪心）
        //    按照深度优先搜索迭代处理，直至可以合并到某个走廊为止，结束本次处理
        //    重复上述操作直至所有分区合并完成

        // 标记数组，记录分区的分配情况
        int[] Pchoose = new int[Partitions.size()];

        // 初始化：这一步可以使得之后的栈操作正确
        // -3：当前分区不允许被合并，不纳入考虑（外部/只进不出/不可进出）
        // -2：当前循环中被选中，在本次合并批次中
        // -1：正常的未选择
        // 非负：分区归属的叶子节点编号
        for (int i = 0; i < Partitions.size(); i++) {
            // 当前分区不允许被合并，不纳入考虑
            if (!Partitions.get(i).getConnect()) {
                Pchoose[i] = -3;
            }
            // 当前分区为Hallway（已有归属）
            else if (Hallway.contains(i)) {
                // 所属叶子节点编号与其在Hallway中的下标一致
                Pchoose[i] = Hallway.indexOf(i);
                Partitions.get(i).setBelong(Hallway.indexOf(i));
            }
            // 一般情况：未分配
            else {
                Pchoose[i] = -1;
            }
        }
        // 第一个：邻居数最少的分区，已知PNumAd中不包含只进不出/无法进出的分区
        int index = PNumAd.get(0).getKey();
        // 还有未处理完的分区（正常来说都有
        while (index != -1) {
            int next = index; // 最后选择
            Stack<Integer> addList = new Stack<>(); // 本次新增分区
            int ban = -1; // 用于控制入栈元素的选择（跳过之前的错误选择）
            int id = -1; // 本轮选择的若干分区待添加的叶子节点id
            addList.push(index);
            int init = index;
            // 深度优先遍历DFS
            while (!addList.isEmpty()) {
                init = index;
                Pchoose[index] = -2; //表示当次选中
                // 此处需要对next是否为-1进行判定：否则后续以next作为下标使用会出问题
                // next = -1：表示当前选择的分区序列无法到达Hallway/已分配分区
                // 假定p0/1/2均暂未归属，其中p0邻居数目最少
                // 邻接情况为 0:(2 1 3) & 1:(0 2) & 2:(1 3 5)，那么addList中从前往后为0 2 1x，1后next为-1无法接续
                next = Partitions.get(index).getMaxAdjacent(Pchoose, ban);
                // 此路不通
                if (next == -1) {
                    // 下次选择更靠后的邻居
                    ban = index;
                    // 撤销选择，弹栈
                    Pchoose[index] = -1;
                    addList.pop();
                    if (addList.isEmpty()) {
                        // 说明当前分区找不到答案
                        System.out.println("p" + init + ":当前分区与任意走廊均不连通！需检查平面图或者调整走廊判定标准");
                    }
                    index = addList.peek();
                } else {
                    // 判定当前选择是否是 Hallway/已分配分区
                    // 选中Hallway而停止，则对应的叶子节点id应为Hallway在走廊数组中的编号
                    if (Hallway.contains(next)) {
                        id = Hallway.indexOf(next);
                        break;
                    }
                    // 选中已分配好的分区而停止，对应叶子节点id与分区对应的叶子节点相同
                    else if (Pchoose[next] >= 0) {
                        id = Pchoose[next];
                        break;
                    } else {
                        // 本次选择成功，分区序列暂时可通，下次选择无禁忌
                        ban = -1;
                        addList.push(next);
                        index = next;
                    }
                }
            }
            // 将本次选择的若干分区加入对应的叶子节点
            for (int add : addList) {
                NodeLeaf nodeLeaf = (NodeLeaf) TreeNode.get(id);
                nodeLeaf.addP(add, Partitions);
                Pchoose[add] = id;
                Partitions.get(add).setBelong(id);
            }
            // 表示当前的分区找不到可合并的走廊
            if (addList.isEmpty()) {
                System.out.println("p" + init + ":当前分区与任意走廊均不连通！需检查平面图或者调整走廊判定标准");
            }
            // 自动规避已有归属的分区
            index = NextPartition(PNumAd, Partitions, Pchoose); // 选择下一个处理分区
        }

        System.out.println("LeafNum = " + TreeNode.size() + "  HallwayNum = " + Hallway.size());

        // ------------------------------------------------------
        // 接下来的内容CreatLeaf和CreatLeaf_Rev应该都是相同的
        // 【启用时如果不放心可以再从CreatLeaf_Rev对应位置复制一遍】

        // 辅助计算结构
        // 构造AD2D:  key--AD_i  value--[key=AD_j, value=AD2ADItem:dist & belong]（包含对应记录的节点id）
        // 2023/10/13补充：该结构与ADM不同，ADM是利用AD2AD计算的结果，AD2AD是前置统计量
        //                AD2AD中保存最短的AD_i→AD_j的距离，以及所属叶子节点id  ！！！！！
        HashMap<Integer, HashMap<Integer, AD2ADItem>> AD2AD = new HashMap<>();
        // 计算受影响信息：key--叶子节点id   value--[key=AD_i,value={AD_j}（受影响的AD门对距离）]
        HashMap<Integer, HashMap<Integer, HashSet<Integer>>> Affect = new HashMap<>();
        // 统计所有叶子节点的AD集合
        HashSet<Integer> LeafAD = new HashSet<>();

        // 4. 计算叶子节点其他成员变量
        System.out.println("------Leaf------");

        /*for(int i=0;i<Pchoose.length;i++){
            System.out.println(i+"-"+Pchoose[i]);
        }*/

        // 维护层次信息
        ArrayList<Integer> levelId = new ArrayList<>();
        for (int i = 0; i < TreeNode.size(); i++) {
            NodeLeaf nodeLeaf = (NodeLeaf) TreeNode.get(i);
            levelId.add(nodeLeaf.getId());

            // 判定访问门，计算邻居，设置归属，计算半径
            nodeLeaf.judgeAD(Doors, Outdoors,ADrecord);
            nodeLeaf.calAdjacent(Doors, Pchoose);
            nodeLeaf.setBelong(Doors);
            nodeLeaf.calLeafRadius(Partitions);

            // 设置包含的分区信息：叶子节点中，cid=Partition
            nodeLeaf.setPartition(nodeLeaf.getCid());

            // 叶子节点信息打印校验
            System.out.println("Node: " + nodeLeaf.getId());
            System.out.println("door: " + nodeLeaf.getD());
            System.out.println("child: " + nodeLeaf.getCid());
            System.out.println("partition: " + nodeLeaf.getPartition());
            System.out.println("isConnect:" + nodeLeaf.getConnect());
            System.out.println("access door: " + nodeLeaf.getAD());
            System.out.println("adjacent:" + nodeLeaf.getAdjacent());

            LeafAD.addAll(nodeLeaf.getAD());
            nodeLeaf.LeafDM(D2D);
            // 根据本节点信息构建AD2AD，AD2AD维护最短AD对距离及所属节点id
            nodeLeaf.getAD2AD(AD2AD, Affect);
            nodeLeaf.judgeSD(Partitions);
            System.out.println();

        }
        NodeLevel.put(1, levelId);

        // 5. 计算ADM：过程几乎和LeafDM计算差不多
        ComputeADM(ADM, AD2AD, LeafAD);

        /*
        System.out.println("--------ADM--------\n");
        for (int i : ADM.keySet()) {
            HashMap<Integer, MItem> ADMitem = ADM.get(i);
            for (int j : ADMitem.keySet()) {
                // 只打印可达信息
                if(ADMitem.get(j).getDistance()!=maxDist){
                    System.out.println(i + "--" + j + ": " + ADMitem.get(j).getDistance() + " " + ADMitem.get(j).getHoop());
                }
            }
            System.out.println();
        }*/

        // 6. 根据ADM和AD2AD更新Affect，通过Affect的记录更新相对应的LeafDM
        UpdateAffect(ADM, AD2AD, Affect);
        for (int changeID : Affect.keySet()) {
            System.out.println("UpdateLeaf: "+changeID);
            NodeLeaf changeNode = (NodeLeaf) TreeNode.get(changeID);
            changeNode.UpdataLeafDM(D2D, ADM, Affect);
            changeNode.resetSD(Partitions);
        }

        /*
        System.out.println("!!!!!!!!Pchoose!!!!!!!\n");
        for(int i=0;i< Pchoose.length;i++){
            System.out.println("pid:"+i+"---"+Pchoose[i]);
        }
        */
    }

    // 建立叶子结点（修复）
    // 2023/10/10 修复版：num(Leaf) ≥ num(Hallway)
    //       如果局部无法联通至走廊，则新建一个叶子节点放入
    //       这部分其实有很多种实现： 最激进——每次都和当前共邻数目最多的分区合并，一旦无法合并至Hallway，则自成一派【√】
    //                           最保守但不是很合理——无法合并的分区独立创建叶子节点
    public void CreatLeaf_Rev(Map<Integer, Integer> PNumAdjacent, ArrayList<Integer> Hallway, ArrayList<Integer> Outdoors) {

        // 1. 初始创建Hallway个节点，每个节点放入一个走廊
        for (int i = 0; i < Hallway.size(); i++) {
            // 此时创建的都为叶子节点
            NodeLeaf nodeLeaf = new NodeLeaf();
            nodeLeaf.setId(i); // 节点id和在Tree中的key值保持相同
            nodeLeaf.addP(Hallway.get(i), Partitions);
            TreeNode.put(i, nodeLeaf);
        }

        // 2. 对PNumAdjacent按value（邻居数）升序排序
        List<Map.Entry<Integer, Integer>> PNumAd = new ArrayList<>(PNumAdjacent.entrySet());
        PNumAd.sort(new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        // System.out.println(PNumAd);

        // 3. 从最少的分区开始处理，每次合并与当前分区共同门数目最多的邻居

        // 标记数组，记录分区的分配情况
        int[] Pchoose = new int[Partitions.size()];
        // 初始化：这一步可以使得之后的栈操作正确
        // -3：当前分区不允许被合并，不纳入考虑（外部/只进不出/不可进出）
        // -2：当前循环中被选中，在本次合并批次中
        // -1：正常的未选择
        // 非负：分区归属的叶子节点编号
        for (int i = 0; i < Partitions.size(); i++) {
            // 当前分区不允许被合并，不纳入考虑
            if (!Partitions.get(i).getConnect()) {
                Pchoose[i] = -3;
            }
            // 当前分区为Hallway（已有归属）
            else if (Hallway.contains(i)) {
                // 所属叶子节点编号与其在Hallway中的下标一致
                Pchoose[i] = Hallway.indexOf(i);
                Partitions.get(i).setBelong(Hallway.indexOf(i));
            }
            // 一般情况：未分配
            else {
                Pchoose[i] = -1;
            }
        }
        // 第一个：邻居数最少的分区，已知PNumAd中不包含只进不出/无法进出的分区
        int index = PNumAd.get(0).getKey();
        // 还有未处理完的分区（正常来说都有
        while (index != -1) {
            int next = index; // 最后选择
            // addList直接使用List结构，没有进出无需使用Stack
            ArrayList<Integer> addList = new ArrayList<>(); // 本次新增分区
            int id = -1; // 本轮选择的若干分区待添加的叶子节点id
            addList.add(index);
            // 未到绝路
            while (next != -1) {
                Pchoose[index] = -2; //表示当次选中
                next = Partitions.get(index).getMaxAdjacent(Pchoose);
                // 正常延伸
                if (next != -1) {
                    // 判定当前选择是否是 Hallway/已分配分区
                    // 选中Hallway而停止，则对应的叶子节点id应为Hallway在走廊数组中的编号
                    if (Hallway.contains(next)) {
                        id = Hallway.indexOf(next);
                        break;
                    }
                    // 选中已分配好的分区而停止，对应叶子节点id与分区对应的叶子节点相同
                    else if (Pchoose[next] >= 0) {
                        id = Pchoose[next];
                        break;
                    } else {
                        // 本次选择成功，继续向下选择
                        addList.add(next);
                        index = next;
                    }
                }
            }
            // 之前的路不通，为此创建一个新叶子节点
            if (next == -1) {
                id = TreeNode.size();
                NodeLeaf nodeLeaf = new NodeLeaf();
                nodeLeaf.setId(id); // 节点id和在Tree中的key值保持相同
                TreeNode.put(id, nodeLeaf);
            }
            // 将本次选择的若干分区加入对应的叶子节点
            for (int add : addList) {
                NodeLeaf nodeLeaf = (NodeLeaf) TreeNode.get(id);
                nodeLeaf.addP(add, Partitions);
                Pchoose[add] = id;
                Partitions.get(add).setBelong(id);
            }

            // 自动规避已有归属的分区
            index = NextPartition(PNumAd, Partitions, Pchoose); // 选择下一个处理分区
        }
        System.out.println("LeafNum = " + TreeNode.size() + "  HallwayNum = " + Hallway.size());

        // ------------------------------------------------------
        // 接下来的内容CreatLeaf和CreatLeaf_Rev应该都是相同的

        // 辅助计算结构
        // 构造AD2D:  key--AD_i  value--[key=AD_j, value=AD2ADItem:dist & belong]（包含对应记录的节点id）
        // 2023/10/13补充：该结构与ADM不同，ADM是利用AD2AD计算的结果，AD2AD是前置统计量
        //                AD2AD中保存最短的AD_i→AD_j的距离，以及所属叶子节点id  ！！！！！
        HashMap<Integer, HashMap<Integer, AD2ADItem>> AD2AD = new HashMap<>();
        // 计算受影响信息：key--叶子节点id   value--[key=AD_i,value={AD_j}（受影响的AD门对距离）]
        HashMap<Integer, HashMap<Integer, HashSet<Integer>>> Affect = new HashMap<>();
        // 统计所有叶子节点的AD集合
        HashSet<Integer> LeafAD = new HashSet<>();

        // 4. 计算叶子节点其他成员变量
        System.out.println("------Leaf------");

        /*for(int i=0;i<Pchoose.length;i++){
            System.out.println(i+"-"+Pchoose[i]);
        }*/

        // 维护层次信息
        ArrayList<Integer> levelId = new ArrayList<>();
        for (int i = 0; i < TreeNode.size(); i++) {
            NodeLeaf nodeLeaf = (NodeLeaf) TreeNode.get(i);
            levelId.add(nodeLeaf.getId());

            // 判定访问门，计算邻居，标记门的归属，计算节点半径
            nodeLeaf.judgeAD(Doors, Outdoors,ADrecord);
            nodeLeaf.calAdjacent(Doors, Pchoose);
            nodeLeaf.setBelong(Doors);
            nodeLeaf.calLeafRadius(Partitions);

            // 设置包含的分区信息：叶子节点中，cid=Partition
            nodeLeaf.setPartition(nodeLeaf.getCid());

            // 叶子节点信息打印校验
            System.out.println("Node: " + nodeLeaf.getId());
            System.out.println("door: " + nodeLeaf.getD());
            System.out.println("child: " + nodeLeaf.getCid());
            System.out.println("partition: " + nodeLeaf.getPartition());
            System.out.println("isConnect:" + nodeLeaf.getConnect());
            System.out.println("access door: " + nodeLeaf.getAD());
            System.out.println("adjacent:" + nodeLeaf.getAdjacent());

            LeafAD.addAll(nodeLeaf.getAD());
            nodeLeaf.LeafDM(D2D);
            // 根据本节点信息构建AD2AD，AD2AD维护最短AD对距离及所属节点id
            nodeLeaf.getAD2AD(AD2AD, Affect);
            // 判定高级门
            nodeLeaf.judgeSD(Partitions);
            System.out.println();

        }
        NodeLevel.put(1, levelId);

        // 5. 计算ADM：过程几乎和LeafDM计算差不多
        ComputeADM(ADM, AD2AD, LeafAD);

        /*
        System.out.println("--------ADM--------\n");
        for (int i : ADM.keySet()) {
            HashMap<Integer, MItem> ADMitem = ADM.get(i);
            for (int j : ADMitem.keySet()) {
                // 只打印可达信息
                if(ADMitem.get(j).getDistance()!=maxDist){
                    System.out.println(i + "--" + j + ": " + ADMitem.get(j).getDistance() + " " + ADMitem.get(j).getHoop());
                }
            }
            System.out.println();
        }*/

        // 6. 根据ADM和AD2AD更新Affect，通过Affect的记录更新相对应的LeafDM
        UpdateAffect(ADM, AD2AD, Affect);
        for (int changeID : Affect.keySet()) {
            System.out.println("UpdateLeaf: "+changeID);
            NodeLeaf changeNode = (NodeLeaf) TreeNode.get(changeID);
            changeNode.UpdataLeafDM(D2D, ADM, Affect);
            changeNode.resetSD(Partitions);
        }

        /*
        System.out.println("!!!!!!!!Pchoose!!!!!!!\n");
        for(int i=0;i< Pchoose.length;i++){
            System.out.println("pid:"+i+"---"+Pchoose[i]);
        }
        */


    }

    // 返回第一个未分配的分区的下标
    public int NextPartition(List<Map.Entry<Integer, Integer>> PNumAd, HashMap<Integer, Partition> P, int[] choose) {
        for (Map.Entry<Integer, Integer> item : PNumAd) {
            int i = item.getKey();
            // 可选择&未选择
            if (P.get(i).getConnect() && choose[i] == -1) {
                return i;
            }
        }
        return -1; // 表示全部选中
    }

    // 返回未选择的距离最近的AD/D的序号
    public int NextDoor(HashMap<Integer, MItem> map, HashSet<Integer> choose) {
        int index = -2;
        double min = maxDist;
        for (int i : map.keySet()) {
            if (!choose.contains(i) && map.get(i).getDistance() < min) {
                index = i;
                min = map.get(i).getDistance();
            }
        }
        return index;
    }

    // 计算全局AD矩阵，保证距离矩阵的正确性
    public void ComputeADM(HashMap<Integer, HashMap<Integer, MItem>> ADM, HashMap<Integer, HashMap<Integer, AD2ADItem>> AD2AD, HashSet<Integer> LeafAD) {
        // ADM计算其实本身还是dijkstra，算法和NodeLeaf中的LeafDM计算差不多
        // 遍历所有AD，从每个点发起dijkstra
        for (int i : LeafAD) {
            HashSet<Integer> choose = new HashSet<>(); // 标记数组
            choose.add(i);
            HashMap<Integer, MItem> map = new HashMap<>();
            ADM.put(i, map);
            // 初始化
            for (int j : LeafAD) {
                MItem mItem = new MItem(maxDist);
                // 默认初始化距离∞，没有下一跳
                map.put(j, mItem);
                if (i == j) {
                    mItem.setDistance(0);
                }
                // 根据AD2AD赋值：此时对于相同的AD_i→AD_j而言，AD2AD中的记录对应的dist不会比D2D更长
                if (AD2AD.containsKey(i) && AD2AD.get(i).containsKey(j)) {
                    mItem.setDistance(AD2AD.get(i).get(j).getDistance());
                }
            }
            // dijkstra计算
            int index = -2;
            // -2的设计在于：在Hoop有使用index=-1（其实到也不是不行
            while ((index = NextDoor(map, choose)) != -2) {
                // 理论上index！=-2表示还有未选择的距离不为∞的点
                choose.add(index);
                HashMap<Integer, AD2ADItem> indexAD;
                if (AD2AD.containsKey(index)) {
                    indexAD = AD2AD.get(index);
                    for (int j : indexAD.keySet()) {
                        // 之前未选过
                        if (!choose.contains(j)) {
                            // 不用+式比较，防止越界（-式比较比较安全）
                            if (map.get(index).getDistance() < map.get(j).getDistance() - indexAD.get(j).getDistance()) {
                                // 更新记录（+式本身其实还有越界风险, 所以要合理设置maxDist）
                                map.get(j).setDistance(map.get(index).getDistance() + indexAD.get(j).getDistance());
                                map.get(j).setHoop(index);
                            }
                        }
                    }
                }
            }

        }

    }

    // ADM计算后，检查AD2AD中受影响的叶子节点和AD对，更新Affect
    public void UpdateAffect(HashMap<Integer, HashMap<Integer, MItem>> ADM, HashMap<Integer, HashMap<Integer, AD2ADItem>> AD2AD, HashMap<Integer, HashMap<Integer, HashSet<Integer>>> Affect) {
        // AD2AD：叶子结点层面的门对距离统计  ADM：全局层面的门对距离统计
        // 检查ADM计算过后，AD2AD中是否有项被改变，如有，应当加入Affect中
        // 虽然通过遍历来发现更新的项其实很愚蠢但是emmmm暂时没想到更好的办法
        for (int i : AD2AD.keySet()) {
            HashMap<Integer, AD2ADItem> AD2ADitem = AD2AD.get(i);
            // i→j
            for (int j : AD2ADitem.keySet()) {
                double old_data = AD2ADitem.get(j).getDistance();
                double new_data = ADM.get(i).get(j).getDistance();
                if (new_data < old_data) {
                    // 说明有记录发生改变，记录对应的所有叶子节点需要重新计算DM
                    HashSet<Integer> changed = AD2ADitem.get(j).getBelong();
                    for (int changedID : changed) {
                        // 调用 NodeLeaf 封装的静态函数
                        NodeLeaf.AddAffect(Affect, changedID, i, j);
                    }
                }
            }
        }

    }

    // 建立非叶子节点
    public void CreatNode() {
        // 上一层连通元素个数：newnum＞NumChild→聚合 // newnum≤NumChild→构成根节点
        int newnum = 0;
        int start = -1;
        int level = 1;
        // 后续用于temptMap起始序号：保证temptMap中序号不重复
        int num = -1;
        ArrayList<Integer> connectNode = new ArrayList<>();
        ArrayList<Integer> levelNode = NodeLevel.get(level);
        for (Integer i : levelNode) {
            Node node = TreeNode.get(i);
            // 计算本层连通的节点个数
            // 不连通（无邻居）的节点不参与构成上层
            if (node.getConnect()) {
                connectNode.add(node.getId());
            }
            // 不连通，直接接入RootNode中
            else {
                RootNode.add(node.getId());
            }
            // 统计最大的id
            if (node.getId() > num) {
                num = node.getId();
            }
        }
        newnum = connectNode.size();
        // 1. 生成非根非叶结点
        while (newnum > NumChild) {
            // 打印上一层的总结信息
            System.out.println("connectNum: " + newnum + " sumNum: " + levelNode.size());
            // 记录本层的AD信息，方便后续DM构建
            HashSet<Integer> NodeAD = new HashSet<>();
            ArrayList<Integer> levelId = new ArrayList<>();
            // 创建处理节点集合
            Map<Integer, MergeTempt> temptMap = new HashMap<>();
            for (int i : connectNode) {
                MergeTempt mergeTempt = new MergeTempt(i, TreeNode.get(i).getAdjacent());
                /*
                System.out.println("Node: "+(i+start)+"\n");
                System.out.println("Cid: "+mergeTempt.getCid());
                System.out.println("Adjacent: "+mergeTempt.getAdjacent());
                 */
                temptMap.put(i, mergeTempt);
            }
            num = num + 1;  // 有效序号 = 下层最大序号+1
            start = num; // 辅助记忆起始序号，防止num改变丢失
            // 自定义比较函数如下：升序排序
            Comparator<Map.Entry<Integer, MergeTempt>> comparator = new Comparator<>() {
                @Override
                public int compare(Map.Entry<Integer, MergeTempt> o1, Map.Entry<Integer, MergeTempt> o2) {
                    // 优先比较度数
                    int i = Integer.compare(o1.getValue().getDegree(), o2.getValue().getDegree());
                    // 度数相同时，比较邻居数目
                    if (i == 0) {
                        i = Integer.compare(o1.getValue().getNumAdjacent(), o2.getValue().getNumAdjacent());
                    }
                    if (i == 0) {
                        i = Integer.compare(o1.getKey(), o2.getKey());
                    }
                    return i;
                }
            };

            // 直接用map/list来模拟堆：操作比较简单，后续可以对此进行优化
            List<Map.Entry<Integer, MergeTempt>> temptList = new ArrayList<>(temptMap.entrySet());
            temptList.sort(comparator);

            /*
            for(Map.Entry<Integer,MergeTempt> item:temptList){
                System.out.println("Node: "+item.getKey());
                System.out.println(item.getValue().getCid());
                System.out.println(item.getValue().getAdjacent()+"\n\n");
            }
            */

            // 循环直到所有节点的度数满足NumChild要求时才停止
            while (temptList.get(0).getValue().getDegree() < NumChild) {
                // 获取待合并的节点序号
                int index1 = temptList.get(0).getKey();
                int index2 = temptList.get(0).getValue().maxAdjacent(); // 具有最大联系的邻居
                // 2023/10/20 补充: 存在局部不连通情况，即index1当前是孤立的，没办法凑齐度数要求
                if (index2 == -1) {
                    // System.out.println("存在局部不连通情况！可手动添加门增加连通性 或者 调低NumChild 或者 进行特殊处理（将当前已聚合的部分生成一个节点保留结果，后续不再参与聚合）");
                    // 额外生成节点保存
                    int extraId = start;
                    System.out.println("存在局部不连通，将当前已聚合的部分生成一个不参与聚合的节点" + extraId);
                    levelId.add(extraId);
                    // 有效编号延后+1
                    start++;
                    Node node = new Node();
                    node.setId(extraId);
                    HashSet<Integer> cid = temptList.get(0).getValue().getCid();
                    node.setCid(cid);
                    // 修改子节点的pid
                    for (int k : cid) {
                        TreeNode.get(k).setPid(extraId);
                        node.addPartiton(TreeNode.get(k).getPartition());
                    }
                    // 可能没有邻居，但是有AD（AD连通外界）
                    node.judgeAD(TreeNode);
                    node.calRadius(TreeNode);
                    NodeAD.addAll(node.getAD());
                    node.setConnect(false);
                    // 无需进行adjacent计算，因为已知当前节点孤立
                    TreeNode.put(extraId, node);
                    // 新增节点孤立，加入RootNode中
                    RootNode.add(extraId);
                    // 删去index1，防止干扰后续
                    temptMap.remove(index1);
                    // 对应更新temptList后，进入下一轮
                    temptList = new ArrayList<>(temptMap.entrySet());
                    temptList.sort(comparator);
                    continue;
                }
                MergeTempt newone = new MergeTempt(num, temptMap.get(index1), temptMap.get(index2));
                // 增加新节点，移除旧节点，更新剩余节点记录，更新排序
                temptMap.remove(index1);
                temptMap.remove(index2);
                for (int i : temptMap.keySet()) {
                    temptMap.get(i).updateAdjacent(index1, index2, num);
                }
                temptMap.put(num, newone);
                temptList = new ArrayList<>(temptMap.entrySet());
                temptList.sort(comparator);
                num++;
            }
            // 此时产生的所有temptList元素均符合度数要求，正常生成节点
            // 纪录新一层节点数目
            int newNode = temptList.size();
            // 创建序号映射记录
            ArrayList<Integer> choose = new ArrayList<>();
            for (int i = 0; i < newNode; i++) {
                choose.add(temptList.get(i).getKey());
            }
            // 创建新一层节点
            level++;
            connectNode.clear();
            num = -1;
            for (int i = 0; i < newNode; i++) {
                int j = start + i;
                // 添加节点&节点记录
                levelId.add(j);
                Node node = new Node();
                node.setId(j);
                HashSet<Integer> cid = temptList.get(i).getValue().getCid();
                node.setCid(cid);
                // 修改子节点的pid
                for (int k : cid) {
                    TreeNode.get(k).setPid(j);
                    node.addPartiton(TreeNode.get(k).getPartition());
                }
                node.judgeAD(TreeNode);
                node.calRadius(TreeNode);
                NodeAD.addAll(node.getAD());
                HashMap<Integer, Integer> adjacent = temptList.get(i).getValue().getAdjacent();
                // adjacent需要进行序号修改后才可以添加
                node.calAdjacent(adjacent, choose, start);
                // 连通，更新newnum和connectNode
                if (node.getConnect()) {
                    connectNode.add(j);
                }
                // 不连通，当前节点加入RootNode
                else {
                    RootNode.add(j);
                }
                if (num < j) {
                    num = j;
                }
                TreeNode.put(j, node);
            }
            NodeLevel.put(level, levelId);
            newnum = connectNode.size();
            System.out.println("\n------level" + level + "------");
            // 计算距离矩阵DM
            for (int i : levelId) {
                Node node = TreeNode.get(i);
                // 打印验证
                System.out.println("Node: " + node.getId());
                System.out.println("child: " + node.getCid());
                System.out.println("partition: " + node.getPartition());
                System.out.println("isConnect:" + node.getConnect());
                System.out.println("access door: " + node.getAD());
                System.out.println("adjacent: " + node.getAdjacent());
                node.NodeDM(TreeNode, ADM, NodeAD,ADrecord);
                System.out.println();
            }
            levelNode = levelId;
        }

        // 2. 生成根节点
        if (newnum > 1) {
            Node root = new Node();
            int rootID = num + 1;
            root.setId(rootID);
            ArrayList<Integer> levelId = new ArrayList<>();
            levelId.add(rootID);
            NodeLevel.put(level + 1, levelId);
            HashSet<Integer> NodeAD = new HashSet<>();
            // 循环结束前已经统计连通节点
            for (int i : connectNode) {
                Node node = TreeNode.get(i);
                node.setPid(rootID);
                root.addCid(i);
                root.addPartiton(node.getPartition());
                NodeAD.addAll(node.getAD());
            }
            root.judgeAD(TreeNode);
            root.calRadius(TreeNode);

            // 打印验证
            System.out.println("\n------Root------");
            System.out.println("Node: " + root.getId());
            System.out.println("child: " + root.getCid());
            System.out.println("partition: " + root.getPartition());
            System.out.println("access door: " + root.getAD());
            root.NodeDM(TreeNode, ADM, NodeAD,ADrecord);
            TreeNode.put(rootID, root);
            RootNode.add(rootID);
            System.out.println();
        }

    }

    // 最短路径查询
    // mode: 模式控制 1--还原最短路径   0--只计算最短距离
    public double ShortPathQuery(Point s,Point t,ArrayList<Integer> path,int mode) {

        int reachtype = ReachType(s, t);
        double dist = maxDist;
        // System.out.println(reachtype);
        // 不连通，不可达
        if (reachtype == -1) {
            System.out.println("s和t不连通 → dist: ∞");
        }
        // s和t位于同一分区中，直接采用欧氏距离（与vita保持一致）
        else if (reachtype == 0) {
            dist = Point.getEuclidDist(s, t);
            System.out.println("s和t位于同一分区中, 直接连通 → dist: "+dist);
        }
        // s和t位于同一叶子中，采用dijkstra计算
        else if (reachtype == 1) {
            dist = ShortPath_inLeaf(s, t, path,mode);
        }
        // s和t位于同一分支不同叶子中，采用回溯迭代计算
        else {
            dist = ShortPath_outLeaf(s, t,path,mode);
        }
        return dist;

    }

    // 读取ShortPathQuery相关的文件
    public void Read_SPQ(String path_SPQ, Point s, Point t) {
        File file = new File(path_SPQ);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split("\t");
                if (!data[0].matches("-?\\d+(\\.\\d+)?")) {
                    continue;
                }
                int pid = Integer.parseInt(data[1]) - 1;
                double x = Double.parseDouble(data[2]);
                double y = Double.parseDouble(data[3]);
                // System.out.println(pid+" "+x+" "+y);
                if (i == 0) {
                    s.setPoint(pid, x, y);
                    i = 1;
                    // System.out.println("建立s");
                }
                // 文件中的有效记录若存在多行，则第一行作为s信息，最后一行作为t信息
                else {
                    t.setPoint(pid, x, y);
                    // System.out.println("建立t");
                }

            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 可达性判定
    // 返回值：-1 不连通，不可达（所在分区不连通//位于不同分支上）
    //        0  在同一分区中
    //        1  在同一叶子节点中
    //        2  不在同一叶子中，但在同一支中
    public int ReachType(Point s, Point t) {
        int p_s = s.getPid();
        int p_t = t.getPid();

        // s和t在同一个分区，未来求解采取欧氏距离
        if (p_s == p_t) {
            return 0;
        } else {
            // s和t不在同一个分区，并且其中某者所在分区不连通
            if (!Partitions.get(p_s).getConnect() || !Partitions.get(p_t).getConnect()) {
                return -1;
            }
            // s和t在同一个叶子节点里，未来计算采取Dijkstra
            else if (Partitions.get(p_s).getBelong() == Partitions.get(p_t).getBelong()) {
                return 1;
            } else {
                // 寻找同时包含p_s和p_t的分支
                for (int i : RootNode) {
                    HashSet<Integer> partition = TreeNode.get(i).getPartition();
                    if (partition.contains(p_s) && partition.contains(p_t)) {
                        return 2;
                    }
                }
                // 不存在这样的分支，即s和t位于不连通的两个分支上
                return -1;
            }
        }
    }

    // 获取s和t对应的Ns和Nt：leaf_s(t)的祖先// LCA(s,t)的孩子
    public void GetNsNt(Point s, Point t, ArrayList<Integer> N) {
        // Ns/Nt初始化为s/t所在叶子结点（已知此时两个叶子结点一定位于同一支）
        // 所以一定能找到对应的Ns/Nt
        int Ns = Partitions.get(s.getPid()).getBelong();
        int Nt = Partitions.get(t.getPid()).getBelong();
        int ancestor_s = TreeNode.get(Ns).getPid();
        int ancestor_t = TreeNode.get(Nt).getPid();
        // 上溯至Ns/Nt的父节点即为LCA(s,t)
        while (ancestor_s != ancestor_t) {
            Ns = ancestor_s;
            Nt = ancestor_t;
            ancestor_s = TreeNode.get(Ns).getPid();
            ancestor_t = TreeNode.get(Nt).getPid();
        }
        // System.out.println("\nNs: "+Ns+" Nt: "+Nt);
        // System.out.println("LCA: "+ancestor_s);
        N.add(Ns);
        N.add(Nt);
    }

    // 计算s到指定祖先节点各个AD的距离
    public void GetDist(Point s, int Ns, HashMap<Integer, MItem> distItem) {
        // 1. 首先计算s到leaf_s各个AD的距离
        int p_s = s.getPid();
        int leaf_s = Partitions.get(p_s).getBelong();
        // System.out.println(p_s+"  "+leaf_s);
        NodeLeaf leaf = (NodeLeaf) TreeNode.get(leaf_s);
        // 获取p_s对应的superdoor
        // 2023/10/22 注意: superdoor 包含 AD&普通门
        //                  但是 distItem中的keySet 只有 AD !!!!!
        HashSet<Integer> SD = leaf.getSD().get(p_s);
        /*System.out.println("------inLeaf "+leaf_s+"------");
        System.out.println("SD: "+SD);
        System.out.println("partitionD: "+Partitions.get(p_s).getD());
        System.out.println("leafAD: " + leaf.getAD());*/
        for (int i : leaf.getAD()) {
            double dist = maxDist;
            int hoop = -1;
            // for(int j:Partitions.get(p_s).getD()){
            for (int j : SD) {
                double dist1 = Point.getEuclidDist(s, Doors.get(j)); // s--j
                double dist2 = leaf.getDM().get(i).get(j).getDistance(); // j--i
                // System.out.println("point → "+j+" → "+i+": "+(dist1+dist2));
                if (dist1 + dist2 < dist) {
                    dist = dist1 + dist2;
                    hoop = j;
                }
            }
            MItem mItem = new MItem(maxDist);
            // hoop与i重合（localDoor），说明此时是直通的
            if (hoop == i) {
                hoop = -1;
            }
            mItem.setDistance(dist);
            mItem.setHoop(hoop);
            distItem.put(i, mItem);
            // System.out.println("Create record: point → "+hoop+" → "+i+": "+dist+"\n");
        }

        // System.out.println("------------");
        // 2. 逐层向上计算与各个祖先节点的距离 (论文 Algorithm2)
        int Nchild = leaf_s;
        int Nparent;
        HashSet<Integer> AD_child;
        HashSet<Integer> AD_parent;
        HashMap<Integer, HashMap<Integer, MItem>> DM;
        // 向上回溯
        while (Nchild != Ns) {
            Nparent = TreeNode.get(Nchild).getPid();
            // System.out.println("------inNode "+Nparent+"------");
            AD_child = TreeNode.get(Nchild).getAD();
            AD_parent = TreeNode.get(Nparent).getAD();
            DM = TreeNode.get(Nparent).getDM();
            for (int i : AD_parent) {
                // 该门未计算过
                if (!distItem.containsKey(i)) {
                    double dist = maxDist;
                    int hoop = -1;
                    for (int j : AD_child) {
                        // j位于下层，之前已经被计算过，加入到distItem中
                        double dist1 = distItem.get(j).getDistance();
                        // 此时的路径为s→j→i
                        double dist2 = DM.get(j).get(i).getDistance();
                        if (dist1 + dist2 < dist) {
                            dist = dist1 + dist2;
                            hoop = j;
                        }
                    }
                    // 这边其实就不太会发生了
                    // 因为 !distItem.containsKey(i) 保证了i!=j
                    //     而hoop一定是某一个j
                    if (hoop == i) {
                        hoop = -1;
                    }
                    MItem mItem = new MItem(maxDist);
                    mItem.setDistance(dist);
                    mItem.setHoop(hoop);
                    distItem.put(i, mItem);
                    // System.out.println("Create record: point → "+hoop+" → "+i+": "+dist);
                }
            }
            // System.out.println();
            // 父子交替向上
            Nchild = Nparent;
        }
        // System.out.println("========");
        /*for(int i:distItem.keySet()){
            System.out.println(i+" "+distItem.get(i).getDistance()+" "+distItem.get(i).getHoop());
        }*/
    }

    // 还原路径
    public ArrayList<Integer> GetPath(ArrayList<Integer> temptPath){
        // 拷贝
        ArrayList<Integer> path = new ArrayList<>(temptPath);
        for(int i=0;i<temptPath.size()-1;i++){
            DecomposePath(temptPath.get(i),temptPath.get(i+1),path);
        }
        return path;
    }

    // 分解某段路径 (论文 Algorithm4)
    public void DecomposePath(int i, int j,ArrayList<Integer> path) {
        // i/j均为普通D时，无需分解
        // 可分解时，i/j至少有一个AD
        // System.out.println(i+"-"+j);
        if (ADrecord.containsKey(i) || ADrecord.containsKey(j)) {
            int nodeid;
            // i/j都是AD
            int hoop = -1;
            if (ADrecord.containsKey(i) && ADrecord.containsKey(j)) {
                // 获取距离矩阵中同时出现i/j的
                ArrayList<Integer> intersect = new ArrayList<>(ADrecord.get(i));
                intersect.retainAll(ADrecord.get(j));
                // intersect中可能存在多个结果，选最小的一个
                // System.out.println("intersect size: " + intersect.size());
                nodeid = Collections.min(intersect);
                hoop = TreeNode.get(nodeid).getDM().get(i).get(j).getHoop();
            }
            // i和j只有一个是AD，此处获取hoop时需要弄清谁是AD，否则会发生空指针异常
            else {
                nodeid = Doors.get(i).getBelong();
                if (nodeid == -1) {
                    System.out.println("代码某位置出错");
                }
                // 说明i是AD
                else if (nodeid == -2) {
                    nodeid = Doors.get(j).getBelong();
                    hoop = TreeNode.get(nodeid).getDM().get(i).get(j).getHoop();
                }
                // 说明j是AD
                else{
                    hoop = TreeNode.get(nodeid).getDM().get(j).get(i).getHoop();
                }
            }
            // System.out.println(nodeid);
            // 可被分解
            // System.out.println(hoop);
            if (hoop != -1) {
                // path细化
                int index = path.indexOf(j);
                System.out.println("Decompose: "+i+"-"+hoop+"-"+j);
                path.add(index, hoop);
                // 递归进行
                DecomposePath(i, hoop, path);
                DecomposePath(hoop, j, path);
            }

        }
    }

    // 同一叶子节点中的最短路径求解
    public double ShortPath_inLeaf(Point s, Point t,ArrayList<Integer> path,int mode) {
        // 获取s和t所在的叶子节点
        Partition p_s = Partitions.get(s.getPid());
        Partition p_t = Partitions.get(t.getPid());
        int leafId = p_s.getBelong();
        HashSet<Integer> door_s = p_s.getD();
        NodeLeaf leaf = (NodeLeaf) TreeNode.get(leafId);

        // 创建dijkstra计算记录
        HashMap<Integer, MItem> distItem = new HashMap<>();
        // 预先添加s-t项，key为-1
        // s和t不在同一个分区，无边直接相连：∞
        MItem tItem = new MItem(maxDist);
        distItem.put(-1, tItem);
        // 初始化，遍历叶子中每一扇门
        HashSet<Integer> door = leaf.getD();
        for (int i : door) {
            // 默认距离设为maxDist
            MItem mItem = new MItem(maxDist);
            // 当前的门与s位于同一分区，可以直接获得距离
            if (door_s.contains(i)) {
                mItem.setDistance(Point.getEuclidDist(s, Doors.get(i)));
            }
            distItem.put(i, mItem);
        }
        HashSet<Integer> choose = new HashSet<>();

        // Dijkstra计算
        int index = -2;
        // index=-2表示所有门都已选完
        // distItem中的项：s所在叶子节点的门+t
        while ((index = NextDoor(distItem, choose)) != -2) {
            choose.add(index);
            // index相关记录
            HashMap<Integer, Double> indexDist = null;
            // 如果是t——就可以退出啦！！！！
            // 因为接下来的计算对s-t而言已经没有意义了
            if (index == -1) {
                break;
            }
            // 如果是某个AD，则就是DM中对应的记录
            else if (leaf.getAD().contains(index)) {
                indexDist = new HashMap<>(); // 需要新建记录
                // 读取AD对应的距离信息
                HashMap<Integer, MItem> indexAD = leaf.getDM().get(index);
                for (int i : indexAD.keySet()) {
                    indexDist.put(i, indexAD.get(i).getDistance());
                }
            }
            // 否则为普通的门，直接从D2D获取项
            else {
                indexDist = new HashMap<>(D2D.get(index));
            }

            // 补充与t相关的项
            // 如果p_t包含index，t与index直接连通
            if (p_t.getD().contains(index)) {
                indexDist.put(-1, Point.getEuclidDist(t, Doors.get(index)));
            }

/*            System.out.println("-----------");
            System.out.println("index: "+index);
            System.out.println("distItem: ");
            for(int k: distItem.keySet()){
                System.out.println(k+": "+distItem.get(k).getDistance());
            }
            System.out.println("indexDist: "+indexDist);
            System.out.println("------------");*/

            for (int i : indexDist.keySet()) {
                // 之前未选择的，包含在distItem中的项
                if (!choose.contains(i) && distItem.containsKey(i)) {
                    if (distItem.get(index).getDistance() + indexDist.get(i) < distItem.get(i).getDistance()) {
                        distItem.get(i).setDistance(distItem.get(index).getDistance() + indexDist.get(i));
                        distItem.get(i).setHoop(index);
                    }
                }
            }
        }

        double dist = distItem.get(-1).getDistance();
        System.out.println("s和t位于同一叶子\ndist: "+dist);
        if(mode==1){
            // 构造初步的path
            ArrayList<Integer> temptPath = new ArrayList<>() ;
            int hoop = distItem.get(-1).getHoop();
            while(hoop!=-1){
                temptPath.add(0,hoop);
                hoop = distItem.get(hoop).getHoop();
            }
            System.out.println("tempt path: "+temptPath);
            // 解析完整的path
            path = GetPath(temptPath);
            System.out.println("final path: "+path);
        }

        return dist;
    }

    // 同分支不同叶子节点中最短路径求解 (论文 Algorithm3)
    public double ShortPath_outLeaf(Point s, Point t,  ArrayList<Integer> path,int mode) {
        // Ns/Nt表示 leaf_s(t)的祖先// LCA(s,t)的孩子
        ArrayList<Integer> N = new ArrayList<>() ;
        GetNsNt(s, t, N);
        HashMap<Integer, MItem> distItem_s = new HashMap<>();
        HashMap<Integer, MItem> distItem_t = new HashMap<>();
        int Ns = N.get(0);
        int Nt = N.get(1);
        GetDist(s, Ns, distItem_s);
        GetDist(t, Nt, distItem_t);
        int LCA = TreeNode.get(Ns).getPid();
        HashMap<Integer, HashMap<Integer, MItem>> DM = TreeNode.get(LCA).getDM();
        int hoop_s = -1;
        int hoop_t = -1;
        double dist = maxDist;
        HashSet<Integer> AD_s = TreeNode.get(Ns).getAD();
        HashSet<Integer> AD_t = TreeNode.get(Nt).getAD();
        // System.out.println(AD_s);
        // System.out.println(AD_t);
        // System.out.println(DM.keySet());
        for (int i : AD_s) {
            double dist1 = distItem_s.get(i).getDistance();
            for (int j : AD_t) {
                double dist2 = distItem_t.get(j).getDistance();
                double dist3 = DM.get(i).get(j).getDistance();

                if (dist1 + dist2+dist3 < dist) {
                    dist = dist1 + dist2 +dist3;
                    hoop_s = i;
                    hoop_t = j;
                    // System.out.println("dist1: "+dist1+" "+"dist2: "+dist2+"dist3: "+dist3+" "+dist);
                }
            }
        }

        System.out.println("s和t位于同一分支不同叶子\ndist: "+dist);
        if(mode==1){
            // 构造初步的path
            ArrayList<Integer> temptPath = new ArrayList<>() ;
            // 中间
            temptPath.add(hoop_s);
            if(hoop_t!=hoop_s){
                temptPath.add(hoop_t);
            }
            System.out.println("hoop_s: "+hoop_s);
            // 左边
            int hoop = distItem_s.get(hoop_s).getHoop();
            while(hoop!=-1){
                // System.out.println(hoop);
                temptPath.add(0,hoop);
                if(!distItem_s.containsKey(hoop)){
                    break;
                }
                hoop = distItem_s.get(hoop).getHoop();
            }
            // 右边
            System.out.println("hoop_t: "+hoop_t);
            hoop = distItem_t.get(hoop_t).getHoop();
            while(hoop!=-1){
                // System.out.println(hoop);
                temptPath.add(hoop);
                if(!distItem_t.containsKey(hoop)){
                    break;
                }
                hoop = distItem_t.get(hoop).getHoop();
            }

            System.out.println("tempt path: "+temptPath);
            path = GetPath(temptPath);
            System.out.println("final path: "+path);
        }
        return dist;
    }

    // ShortPathQuery的验证函数
    // 这部分与ShortPath_inLeaf几乎一模一样
    public double ShortPathCheck(ArrayList<Integer> path,String path_SPQ){
        double dist = maxDist;
        Point s = new Point();
        Point t = new Point();
        // 读入文件
        Read_SPQ(path_SPQ, s, t);
        int p_s = s.getPid();
        int p_t = t.getPid();
        HashSet<Integer> door_s = Partitions.get(p_s).getD();
        HashSet<Integer> door_t = Partitions.get(p_t).getD();

        // 创建dijkstra计算记录
        HashMap<Integer, MItem> distItem = new HashMap<>();
        // 添加s-t
        MItem tItem = new MItem(maxDist);
        // s/t位于同一分区，直接连通
        if(p_s==p_t){
            tItem.setDistance(Point.getEuclidDist(s,t));
        }
        distItem.put(-1, tItem);

        // 初始化，遍历每一扇门
        HashSet<Integer> door = new HashSet<>(Doors.keySet());
        for (int i : door) {
            // 默认距离设为maxDist
            MItem mItem = new MItem(maxDist);
            // 当前的门与s位于同一分区，可以直接获得距离
            if (door_s.contains(i)) {
                mItem.setDistance(Point.getEuclidDist(s, Doors.get(i)));
            }
            distItem.put(i, mItem);
        }
        HashSet<Integer> choose = new HashSet<>();
        // Dijkstra计算
        int index = -2;
        // index=-2表示所有门都已选完
        // distItem中的项：s所在叶子节点的门+t
        while ((index = NextDoor(distItem, choose)) != -2) {
            choose.add(index);
            // index相关记录
            // 如果是t——就可以退出啦！！！！
            // 因为接下来的计算对s-t而言已经没有意义了
            if (index == -1) {
                break;
            }

            HashMap<Integer, Double> indexDist = new HashMap<>(D2D.get(index));
            // 补充与t相关的项
            // 如果p_t包含index，t与index直接连通
            if (door_t.contains(index)) {
                indexDist.put(-1, Point.getEuclidDist(t, Doors.get(index)));
            }

/*            System.out.println("-----------");
            System.out.println("index: "+index);
            System.out.println("distItem: ");
            for(int k: distItem.keySet()){
                System.out.println(k+": "+distItem.get(k).getDistance());
            }
            System.out.println("indexDist: "+indexDist);
            System.out.println("------------");*/

            for (int i : indexDist.keySet()) {
                // 之前未选择的，包含在distItem中的项
                if (!choose.contains(i) && distItem.containsKey(i)) {
                    if (distItem.get(index).getDistance() + indexDist.get(i) < distItem.get(i).getDistance()) {
                        distItem.get(i).setDistance(distItem.get(index).getDistance() + indexDist.get(i));
                        distItem.get(i).setHoop(index);
                    }
                }
            }
        }
        dist = distItem.get(-1).getDistance();
        int hoop = distItem.get(-1).getHoop();
        while(hoop!=-1){
            path.add(0,hoop);
            hoop = distItem.get(hoop).getHoop();
        }
        if(dist==maxDist){
            System.out.println("s和t不连通");
        }
        else{
            System.out.println("dist: "+dist);
            System.out.println("path:" +path);
        }
        return dist;
    }

    // 在D2D基础上通过dijkstra计算出全局DM，得到的结果可用于验证算法正确性
    public void DMCheck(){

        String filePath = "dataset/D2D-Dijkstra.txt"; // 文件路径
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            HashSet<Integer> door = new HashSet<>(Doors.keySet());
            for(int i:door){
                // 创建dijkstra计算记录
                HashMap<Integer, MItem> distItem = new HashMap<>();
                for (int j : door) {
                    // 默认距离设为maxDist
                    MItem mItem = new MItem(maxDist);
                    // 当前的门与s位于同一分区，可以直接获得距离
                    if(i==j){
                        mItem.setDistance(0);
                    }
                    else if(D2D.containsKey(i)&&D2D.get(i).containsKey(j)){
                        mItem.setDistance(D2D.get(i).get(j));
                    }
                    distItem.put(j, mItem);
                }
                HashSet<Integer> choose = new HashSet<>();
                choose.add(i);
                int index = -2;
                while ((index = NextDoor(distItem, choose)) != -2) {
                    choose.add(index);

                    HashMap<Integer, Double> indexDist = new HashMap<>(D2D.get(index));
                    for (int k : indexDist.keySet()) {
                        // 之前未选择的，包含在distItem中的项
                        if (!choose.contains(k) && distItem.containsKey(k)) {
                            if (distItem.get(index).getDistance() + indexDist.get(k) < distItem.get(k).getDistance()) {
                                distItem.get(k).setDistance(distItem.get(index).getDistance() + indexDist.get(k));
                                distItem.get(k).setHoop(index);
                            }
                        }
                    }
                }
                for(int j:door){
                    if(distItem.get(j).getDistance()<maxDist){
                        writer.write(Integer.toString(i)+" "+Integer.toString(j)+" "+Double.toString(distItem.get(j).getDistance())+"  "+Integer.toString(distItem.get(j).getHoop())+"\n");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        // 实例化
        TreeApp treeApp = new TreeApp();

        treeApp.DMCheck();

        Point s = new Point();
        Point t = new Point() ;
        treeApp.Read_SPQ("dataset/s-t-1.txt", s, t);
        System.out.println(treeApp.ShortPathQuery(s,t,null,0));
        ArrayList<Integer> checkPath = new ArrayList<>() ;
        treeApp.ShortPathCheck(checkPath,"dataset/s-t-1.txt");


    }
}
