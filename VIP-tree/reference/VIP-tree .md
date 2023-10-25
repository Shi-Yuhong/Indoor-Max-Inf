## VIP-tree 复现笔记

*本文为复现时的一些思考，随写随记，为了方便日后重读代码时更容易上手*

***注意：当复现基本完成时，需要重新整理TreeApp中的成员（具有存储价值）***

​                                              ***还可以考虑一下封装问题（把某些函数再降级到更低层次的ADT）***

### 叶子节点

#### 生成

>​    Two partitions are called *adjacent* partitions if they have at least one common door (e.g., *P*1 and *P*2). We iteratively merge adjacent partitions and construct the leaf nodes by considering the following two simple rules.
>
>> **i.** If a general partition has more than one adjacent hallways, **it is merged with the hallway with greater number of common doors with the general partition**. Ties are broken by preferring the hallway that is on the same floor. If the general partition occupies more than one floors (e.g., it is a staircase) or if both hallways are on the same floor, the tie is broken arbitrarily.
>
>> **ii.** **Merging of a partition with a leaf node is not allowed if the merging will result in a leaf node having more than one hallways.** This is because the shortest distance/path queries between points in different hallways are more expensive. This rule ensures that all hallways are in different leaf nodes, which allows us to fully leverage the tree structure to effificiently process the queries. The algorithm terminates when no further merging is possible, i.e., **everypossible merging will result in the violation of this rule**.

​    根据论文此处的意思，应有`num(Leaf) = num(Hallway)`，但是文中给出的组合方法感觉比较迷惑模糊，而且理论上这个组合方法产生的结果是**不稳定**的（每次运行结果不一定完全相同），再者，最后产生的叶子节点规模有可能差别很大（即**负载不均衡**：内部门的数量过多 / 暴露在外的门过多），但是目前我没有很好的思路来解决，这一块代码是可以再商榷思考的。

​    我的想法大致如下：先对当前分区按照邻居数升序排序，优先处理邻居数少的分区（类似Algorithm 1：**贪心+DFS**），合并其共邻数目最大的邻居，并将处理标记修改为true，重复此操作，直到合并到Hallway为止，然后再处理下一个分区。

​    *老实说，有点像聚类的意思（但是显然希望以更小的代价来处理）。*

#### 矩阵计算

> Recall that the distance matrix for a leaf node *N* stores the distance and the next
>hop door on the shortest path between every door *di∈N* to every access door *dj∈AD(N)*. We compute these distances and the next hop doors using **Dijkstra’s search on the D2D graph**. Specififically, for each access door *dj* of a leaf node *N*, we issue a Dijkstra’s search until all doors in the node *N* are reached. Since the doors of
>the leaf nodes are close to each other, this Dijkstra’s search is quite cheap as **only the nearby nodes in the D2D graph are visited**.

​    此处论文只是简要说明了 “在D2D上展开dijkstra” 就可实现叶子节点的矩阵计算，但是会产生一个问题，此处的解法只适用于 **最短路径只经过节点内部** 的情况，并不适用于后期补充的特例。

<img src="C:\Users\Awu\Desktop\VIP-tree\reference\image\special.png" style="zoom:50%;" />

> ​    d2到d5的最短路径：d2→d3→d4→d5，为了后期分解路径的方便，下一跳门只可以设计为d4，即 **最短路径如果经过节点外，则下一跳门为路径上的第一扇AD** 

​    简单的在叶子节点内部展开dijkstra，显然无法处理上述情况，即该种处理只知晓节点内部情况，不掌握外部情况，**默认最短路径只在节点内部**，显然是矛盾的。

​    我曾想过，首先在全局用dijkstra计算各个叶子节点AD之间的距离，但此时若要得出最正确的结果，需要利用单个节点的AD之间在其内部的最短距离，这显然又需要先在节点内展开dijkstra，和设计矛盾。

​    所以我给出的解决思路如下：最短路径经过节点外应该只是**特例**，出现概率较低。可以按照正常思路求解，计算完叶子节点的距离矩阵后，再计算AD之间的距离，如果出现**某个节点自身包含的AD之间的最短距离发生变化**，则说明有可能产生特例情况，需重新更新叶子节点的距离矩阵*（感觉真的麻烦极了，但是似乎没有完全的解决办法）*。

> **Dijkstra细节**
>
> <img src="C:\Users\Awu\Desktop\VIP-tree\reference\image\partition.png" style="zoom:80%;" />
>
> <img src="C:\Users\Awu\Desktop\VIP-tree\reference\image\tree.png" style="zoom:80%;" />
>
> ​    根据图片的对应关系，下一跳门其实就是P数组最后更新的结果（**源点到当前顶点的最短路径上，最后经过的顶点**）。
>
> ​    同时还要注意计算**越界**问题：当max_value取计算上限时，max_value+1=min_value，可能会因此造成错误判断。我感觉合理的解决办法是选取一个理论上界，即实际应用中取值不会超过该值，反复进行加法计算也不容易越界（但是很明显麻烦很多）。

### 非叶子结点

#### 生成

<img src="C:\Users\Awu\Desktop\VIP-tree\reference\image\algorithm1.png" style="zoom:85%;" />

​    关于非叶子结点的生成，论文给出了具体的算法，总结如下：

>由l层节点创建l+1层节点  *^t：非根非叶节点最小度（可设参数）^* 
>
>> ①  把所有节点都插入Min-Heap（key=degree）中 
>>           初始时所有的节点degree=1    ^*未与其他定点合并*^ 
>
>> ②  当top.degree＜t时：    *^保证循环结束时符合t条件^*         
>>           取堆顶Ni，找到与Ni共同门数最多的Nj        ^*尽量使AD(Parent)更少*^      
>>       （如果两个节点具有相同degree，优先选择相邻分区少的节点参与合并）        
>>           合并为Nk（k.degree=i.degree+j.degree），插入堆中 
> 
>> ③  把所有Min-Heap中节点加入x+1层
