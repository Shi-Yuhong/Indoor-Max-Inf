package dataStruct;

import java.util.HashMap;
import java.util.HashSet;

// 此类作为中间节点生成的过渡形态
public class MergeTempt {

    private int id;
    private HashSet<Integer> cid;
    private HashMap<Integer,Integer> Adjacent;

    // 根据下层Tree节点构建
    public MergeTempt( int id, HashMap<Integer, Integer> adjacent) {
        this.id = id;
        cid = new HashSet<>();
        cid.add(id); // 初始时，自己是自己的子节点
        // 防御性拷贝（如果对象指针指向相同，后期也许会有麻烦）
        Adjacent = new HashMap<>(adjacent);
    }

    // 由两个MergeTempt合并生成
    public MergeTempt(int id,MergeTempt m1,MergeTempt m2){
        this.id = id;
        // cid直接添加m1和m2的cid（set自动剔除重复元素）
        cid = new HashSet<>();
        cid.addAll(m1.getCid());
        cid.addAll(m2.getCid());
        Adjacent = new HashMap<>();
        HashMap<Integer,Integer> ad1 = m1.getAdjacent();
        HashMap<Integer,Integer> ad2 = m2.getAdjacent();
        // 遍历ad1，添加其中项
        for(int i:ad1.keySet()){
            Adjacent.put(i,ad1.get(i));
        }
        // 遍历ad2，重复项直接加和，否则新增
        for(int j:ad2.keySet()){
            if(Adjacent.containsKey(j)){
                int temp = Adjacent.get(j);
                Adjacent.replace(j,temp+ad2.get(j)); // 更新
            }
            else{
                Adjacent.put(j,ad2.get(j)); // 新增
            }
        }
        // 删掉关于m1和m2的记录
        Adjacent.remove(m1.getId());
        Adjacent.remove(m2.getId());
    }

    public int getId() {
        return id;
    }

    public HashSet<Integer> getCid() {
        return cid;
    }

    public HashMap<Integer, Integer> getAdjacent() {
        return Adjacent;
    }

    // 获取度数
    public int getDegree(){
        return cid.size();
    }

    public int getNumAdjacent(){
        return Adjacent.size();
    }
    // 返回共同门数目最多的节点，用于合并
    public int maxAdjacent(){
        int index = -1;
        int max = -1;
        if(Adjacent.size()==0){
            return index;
        }
        for(int i:Adjacent.keySet()){
            if(Adjacent.get(i)>max){
                max = Adjacent.get(i);
                index = i;
            }
        }
        return index;
    }

    // 更新Adjacent记录
    public void updateAdjacent(int index1,int index2,int num){
        if(Adjacent.containsKey(index1)&&Adjacent.containsKey(index2)){
            int newdata = Adjacent.get(index1)+Adjacent.get(index2);
            Adjacent.put(num,newdata);
            Adjacent.remove(index1);
            Adjacent.remove(index2);
        }
        else if(Adjacent.containsKey(index1)){
            int newdata = Adjacent.get(index1);
            Adjacent.put(num,newdata);
            Adjacent.remove(index1);
        }
        else if(Adjacent.containsKey(index2)){
            int newdata = Adjacent.get(index2);
            Adjacent.put(num,newdata);
            Adjacent.remove(index2);
        }
    }
}
