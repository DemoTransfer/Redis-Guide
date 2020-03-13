看在前面
====

> * <a href="https://gitbook.cn/books/5aa4cb50c2ff6f2e120891d0/index.html">基于 Redis 的分布式缓存实现方案及可靠性加固策略</a>

内容提要
====

* Redis 简介；

* Redis 单机模式及原理；

* Redis 集群模式及原理；

* Redis 故障转移；

* Redis 扩容；

* 搭建一个基于 Redis 和 Lettuce 的分布式缓存集群；

* Redis“踩坑”案例。

从“端到端”的角度写一篇文章——Redis 集群作为分布式缓存服务端，基于 Lettuce 构建客户端，详细讲述搭建一套分布式缓存的流程。文末给出了几个使用 Redis 的踩坑实例

一、Redis 简介
====

关于 Redis，介绍类的文章已经很多，本文不再详述，仅作简要介绍。Redis 是开源免费的高性能 Key-V alue数据库。与我们熟知的关系型数据库 Oracle、Microsoft SQLServer、MySQL 不同，Redis 属于 NoSQL 数据库（非关系数据库），Redis 不使用表，它的数据库也不会预定义或者强制去要求用户对 Redis 存储的不同数据进行关联。

二、Redis特点
====

与其它 Key-Value 缓存产品相比，Redis 有以下特点：

* Redis 支持数据的持久化（包括 AOF 和 RDB 两种模式），可以将内存中的数据保存在磁盘中，重启的时候可以再次加载进行使用，性能与可靠性兼顾；

* Redis 不是仅仅支持简单的 Key-Value类型的数据，还支持字符串、列表、集合、散列表、有序集合数据结构的存储，这一优势使 Redis 适用于更广泛的应用场景；

* Redis 支持数据的备份，即 Master-Slave 模式，Master 故障时，对应的 Slave 将通过选举升主，保障可用性；

* Redis 主进程是单线程工作，因此，Redis 的所有操作都是原子性的，即操作要么成功执行要么失败完全不执行。单个操作是原子性的。多个操作也支持事务，即原子性；

* Redis 性能优越，读的速度达110000次/s，写的速度达81000次/s。

除了上述特点，Redis 还支持 Publish/Subscribe，通知，Key 老化逐出等特性。

三、Redis 单机模式
====

顾名思义，单机模式就是指 Redis 主节点以单个节点的形式存在，这个主节点可读可写，上面存储数据全集。在3.0版本之前，Redis 只能支持单机模式，出于可靠性考量，通常单机模式为“1主 N 备”的结构，如下所示：

![Redis单机模式结构一]()

需要说明的是，即便有很多个 Redis 主节点，只要这些主节点以单机模式存在，本质上仍为单机模式。单机模式比较简单，足以支撑一般应用场景，但单机模式具有固有的局限性：不支持自动故障转移，扩容能力极为有限，高并发瓶颈。

* 不支持自动故障转移

Redis 单机模式下，即便是“1主 N 备”结构，当主节点故障时，备节点也无法自动升主，即无法自动故障转移。故障转移需要“哨兵”Sentinel 辅助，Sentinel 是 Redis 高可用的解决方案，由一个或者多个 Sentinel 实例组成的系统可以监视 Redis 主节点及其从节点，当检测到 Redis 主节点下线时，会根据特定的选举规则从该主节点对应的所有从节点中选举出一个“最优”的从节点升主，然后由升主的新主节点处理请求。具有 Sentinel 系统的单机模式示意图如下：

![Sentinel 系统的单机模式示意图]()

* 扩容能力极为有限

这一点应该很好理解，单机模式下，只有主节点能够写入数据，那么，最大数据容量就取决于主节点所在物理机的内存容量，而物理机的内存扩容（Scale-Up）能力目前仍是极为有限的。

* 高并发瓶颈

由于 Redis 主进程是单线程工作的，并发支持能力瓶颈明显。

四、Redis 集群模式
====

单实例 Redis 虽然简单，但瓶颈明显。一是容量问题，在一些应用场景下，数据规模可达数十 G，甚至数百 G，而物理机的资源却是有限的，内存无法无限扩充；二是并发性能问题，Redis 号称单实例10万并发，但也仅仅是10万并发。鉴于单机模式的局限性，Redis 集群模式应运而生。

<h3>Redis 集群实现基础</h3>

Redis 集群实现的基础是分片，即将数据集有机的分割为多个片，并将这些分片指派给多个 Redis 实例，每个实例只保存总数据集的一个子集。利用多台计算机内存和来支持更大的数据库，而避免受限于单机的内存容量；通过多核计算机集群，可有效扩展计算能力；通过多台计算机和网络适配器，允许我们扩展网络带宽。

![Redis集群一]()

Redis 集群分片的几种实现方式如下：

* 查询路由（Query Routing）

意味着，你可以发送你的查询到一个随机实例，这个实例会保证转发你的查询到正确的节点。Redis-Cluster 在客户端的帮助下，实现了查询路由的一种混合形式（请求不是直接从 Redis 实例转发到另一个，而是客户端收到重定向到正确的节点）。

* 客户端分片（Client Side Partitioning）

意味着，客户端直接选择正确的节点来写入和读取指定键。许多 Redis 客户端实现了客户端分片。

* 代理协助分片（Proxy Assisted Partitioning）

意味着，我们的客户端发送请求到一个可以理解 Redis 协议的代理上，而不是直接发送请求到 Redis 实例上。代理会根据配置好的分片模式，来保证转发我们的请求到正确的 Redis 实例，并返回响应给客户端。Redis 和 Memcached 的代理 Twemproxy 实现了代理协助的分片。

<h3>Redis-cluster</h3>

从3.0版本开始，Redis 支持集群模式——Redis-Cluster，可线性扩展到1000个节点。Redis-Cluster 采用无中心架构，每个节点都保存数据和整个集群状态，每个节点都和其它所有节点连接。Redis-Cluster 架构图如下所示。

1. Redis 官方出品，可线性扩展到1000个节点；

2. 无中心架构：每个节点都保存数据和整个集群状态，每个节点都和其它所有节点连接；

3. 一致性哈希思想；

4. 客户端直连 Redis 服务，免去了 Proxy 代理的损耗。

<h3>Redis-Cluster 原理</h3>

1. Hash slot

基于“分片”的思想，Redis 提出了 Hash Slot。Redis-Cluster 把所有的物理节点映射到预分好的16384个 Slot，当需要在 Redis 集群中放置一个 Key-Value 时，根据 CRC16(key) Mod 16384的值，决定将一个 Key 放到哪个 Slot 中。

2. 集群内的每个 Redis 实例监听两个 TCP 端口，6379（默认）用于服务客户端查询，16379（默认服务端口 + 10000）用于集群内部通信。

3. 节点间状态同步：Gossip 协议，最终一致性。所有的 Redis 节点彼此互联（PING-PONG机制），节点间通信使用轻量的二进制协议，减少带宽占用。

<h3>Redis-Cluster 请求路由方式</h3>

客户端直连 Redis 服务，进行读写操作时，Key 对应的 Slot 可能并不在当前直连的节点上，经过“重定向”才能转发到正确的节点。如下图所示，我们直接登陆127.0.0.1:6379客户端，进行 set 操作，当 Key 对应的 Slot 不在当前节点时（如 key-test)，客户端会报错并返回正确的节点的 IP 和端口。

![Redis-cluster二](https://github.com/DemoTransfer/RedisGuide/blob/master/document/picture/Redis-cluster%E4%BA%8C.png)

以集群模式登陆127.0.0.1:6379客户端（注意命令的差别：-c 表示集群模式)，则可以清楚的看到“重定向”的信息，并且客户端也发生了切换：“6379”->“6381”。

![Redis-cluster三](https://github.com/DemoTransfer/RedisGuide/blob/master/document/picture/Redis-cluster%E4%B8%89.png)

以三节点为例，上述操作的路由查询流程示意图如下所示：

![Redis-cluster四]()

和普通的查询路由相比，Redis-Cluster 借助客户端实现的请求路由是一种混合形式的查询路，它并非从一个 Redis 节点到另外一个 Redis，而是借助客户端转发到正确的节点。

在实际应用中，可以在客户端缓存 Slot 与 Redis 节点的映射关系，当接收到 MOVED 响应时修改缓存中的映射关系。如此，基于保存的映射关系，请求时会直接发送到正确的节点上，从而减少一次交互。

目前，包括 Lettuce（下文将介绍），Jedis 在内的许多 Redis Client，都已经实现了对 Redis-Cluster 的支持。

<h3>客户端分片</h3>

* 由客户端决定 Key 写入或者读取的节点。

* 包括 Lettuce、Jedis 在内的一些客户端，实现了客户端分片机制。

![Redis-cluster五]()

采用客户端分片具有逻辑简单，性能高的优点，但缺点也很明显：业务逻辑与数据存储逻辑耦合，可运维性差；多业务各自使用 Redis，集群资源难以管理，不支持动态增删节点。

<h3>基于代理的分片</h3>

为了克服客户端分片业务逻辑与数据存储逻辑耦合的不足，可以通过 Proxy 将业务逻辑和存储的逻辑隔离。客户端发送请求到一个代理，代理解析客户端的数据，将请求转发至正确的节点，然后将结果回复给客户端。这种架构还有一个优点就是可以把 Proxy 当成一个中间件，在这个中间件上你可以做任何事情，比如可以把集群和主从的兼容性做到几乎一致、可以做无缝扩缩容、安全策略等等。

基于代理的分片已经有很多成熟的方案，如开源的 Codis，此外，阿里云 ApsaraDB for Redis/ApsaraCache 等大企业很多也是采用 Proxy+Redis-server 的架构。

基本原理如下图所示：

![Redis-cluster六](https://github.com/DemoTransfer/RedisGuide/blob/master/document/picture/Redis-cluster%E5%85%AD.png)

五、Redis 故障转移
====

在上面已经介绍过单机模式的故障转移（主节点下线后，对应从节点升主并替代原主节点继续工作的过程），单机模式下故障转移需要 Sentinel 系统的辅助，与之不同，Redis 集群模式故障转移并不需要 Sentinel 系统辅助，而是通过集群内部主节点选举完成，是一个“自治”的系统，以下详细介绍。

<h3>故障检测</h3>

1. 单点视角检测

集群中的每个节点都会定期通过集群内部通信总线向集群中的其它节点发送 PING 消息，用于检测对方是否在线，如果接收 PING 消息的节点没有在规定的时间内向发送 PING 消息的节点返回 PONG 消息，那么，发送 PING 消息的节点就会将接收 PING 消息的节点标注为疑似下线状态（Probable Fail，Pfail）。

2. 检测信息传播

集群中的各个节点会通过相互发送消息的方式来交换自己掌握的集群中各个节点的状态信息，如在线，疑似下线（Pfail），下线（fail）。例如，当一个主节点 A 通过消息得知主节点 B 认为主节点 C 疑似下线时，主节点 A 会更新自己保存的集群状态信息，将从 B 获得的下线报告保存起来。

3. 基于检测信息作下线判决

如果在一个集群里，超过半数的处理槽的主节点都将某个主节点 X 报告为疑似下线，那么，主节点 X 将被标记为下线（fail），并广播出去，所有收到这条 fail 消息的节点都会立即将主节点 X 标记为 fail。至此，故障检测完成。

<h3>选举</h3>

1. 从节点拉票

基于故障检测信息的传播，集群中所有正常节点都将感知到某个主节点下线的信息，当然也包括这个下线主节点的所有从节点。当从节点发现自己的复制的主节点状态为已下线时，从节点就会向集群广播一条请求消息，请求所有收到这条消息并且具有投票权的主节点给自己投票。

2. 拉票优先级

严格的讲，从节点在发现其主节点下线时，并不是立即发起故障转移流程而进行“拉票”的，而是要等待一段时间，在未来的某个时间点才发起选举。这个时间点有如下计算表达式：其中，固定延时500ms，是为了留出时间，使主节点下线的消息能传播到集群中其他节点，这样集群中的主节点才有可能投票；随机延时是为了避免两个从节点同时开始故障转移流程；rank 表示从节点的排名，排名是指当前从节点在下线主节点的所有从节点中的排名，排名主要是根据复制数据量来定，复制数据量越多，排名越靠前，因此，具有较多复制数据量的从节点可以更早发起故障转移流程，从而更可能成为新的主节点。

```java
mstime() + 500ms + random()%500ms + rank*1000ms
```

3. 主节点投票

如果一个主节点具有投票权（负责处理 Slot 的主节点)，并且这个主节点尚未投票给其它从节点，那么这个主节点将向请求投票的从节点返回一条回应消息，表示支持该从节点升主。

4. 根据投票结果决策

在一个具有 N 个主节点投票的集群中，理论上每个参与拉票的从节点都可以收到一定数量的主节点投票，但是，在同一轮选举中，只可能有一个从节点收到的票数大于 N/2 + 1，也只有这个从节点可以升级为主节点，并代替已下线的主节点继续工作。

5. 选举失败

跟生活中的选举一样，选举可能失败——没有一个候选从节点获得超过半数的主节点投票。遇到这种情况，集群将会进入下一轮选举，直到选出新的主节点为止。

6. 选举算法

选举新的主节点的算法是基于 Raft 算法的 Leader Election 方法来实现的，本文就不展开了。

<h3>故障转移</h3>

1. 身份切换

通过选举晋升的从节点会执行一系列的操作，清除曾经为从的信息，改头换面，成为新的主节点。

2. 接管职权

新的主节点会通过轮询所有 Slot，撤销所有对已下线主节点的 Slot 指派，消除影响，并且将这些 Slot 全部指派给自己。

3. 广而告之

升主了嘛，必须让圈子里面的都知道，新的主节点会向集群中广播一条 PONG 消息，将自己升主的信息通知到集群中所有节点。

4. 履行义务

在其位谋其政，新的主节点开始处理自己所负责 Slot 对应的请求，至此，故障转移完成。

六、Redis 扩容
====

随着应用场景的升级，缓存可能需要扩容，扩容的方式有两种：纵向扩容（Scale Up）和横向扩容（Scale Out)。纵向扩容可以通过客户端命令：config set maxmemory xxx 实现，无需详述。实际应用场景中，采用横向扩容更多一些，根据是否增加主节点数量，横向扩容方式有两种。

1. 主节点数量不变

比如，当前有一台物理机 A，构建了一个包含3个 Redis 实例的集群；扩容时，我们新增一台物理机 B，拉起一个 Redis 实例并加入物理机 A 的集群；B 上 Redis 实例对 A 上的一个主节点进行复制，然后进行主备倒换；如此，Redis 集群还是3个主节点，只不过变成了 A2-B1 的结构，将一部分请求压力分担到了新增的节点上，同时物理容量上限也会增加，主要步骤如下：

1）. 将新增节点加入集群；

2）. 将新增节点设置为某个主节点的从，进而对其进行复制；

3）. 进行主备倒换，将新增的节点调整为主。

2. 增加主节点数量

不增加主节点数量的方式扩容比较简单，但是，从负载均衡的角度来看，并不是很好的选择。例如，如果主节点数量较少，那么单个节点所负责的 Slot 的数量必然较多，很容易出现大量 Key 的读写集中于少数节点的现象，而增加主节点的数量，可以更有效的分摊访问压力，充分利用资源。主要步骤如下：

1）. 将新增节点加入集群；

2）. 将集群中的部分 Slot 迁移至新增的节点。

七、基于 Redis 和 Lettuce 搭建一个端到端的分布式缓存集群
====

本章，我将介绍基于 Redis 和 Lettuce 搭建一个分布式缓存集群的方法。为了生动的呈现集群创建的过程，我没有采用 Redis 集群管理工具 redis-trib，而是基于 Lettuce 编写 Java 代码实现集群的创建，相信，这将有利于读者更加深刻的理解 Redis 集群模式。

<h3>方案简述</h3>

Redis 集群模式至少需要3个主节点，作为举例，本文搭建一个3主3备的精简集群，麻雀虽小，五脏俱全。

主备关系如下图所示，其中 M 代码 Master 节点，S 代表 Slave 节点，A-M 和 A-S 为一对主备节点。

![Redis-cluster七](https://github.com/DemoTransfer/RedisGuide/blob/master/document/picture/Redis-cluster%E4%B8%83.png)

按照上图所示的拓扑结构，如果节点1故障下线，那么节点2上的 A-S 将升主为 A-M，Redis 3节点集群仍可用，如下图所示：

![Redis-cluster八](https://github.com/DemoTransfer/RedisGuide/blob/master/document/picture/Redis-cluster%E5%85%AB.png)

**特别说明**：事实上，Redis 集群节点间是两两互通的，如下图所示，上面作为示意图，进行了适当简化。

![Redis-cluster九](https://github.com/DemoTransfer/RedisGuide/blob/master/document/picture/Redis-cluster%E4%B9%9D.png)

<h3>资源准备</h3>

(1) 下载 Redis 包：前往 Redis 官网下载 Redis 资源包，本文采用的 Redis 版本为4.0.8。

(2) 将下载的 Redis 资源包 redis-3.2.11.tar.gz 放到自定义目录下，解压，编译便可以生成 Redis 服务端和本地客户端 bin 文件 redis-server 和 redis-cli，具体操作命令如下：

```java
tar xzf redis-4.0.8.tar.gz
cd redis-4.0.8
make
```

(3) 编译完成后，在 src 目录下可以看到生成的 bin 文件 redis-server 和 redis-cli。

<h3>集群配置</h3>

写作本文时，手头只有一台机器，无法搭建物理层面的3节点集群，限于条件，我在同一台机器上创建6个 Redis 实例，构建3主3备。

(1) 创建目录

根据端口号分别创建名为6379，6380，6381，6382，6383，6384的文件夹。

(2) 修改配置文件

在解压文件夹 redis-4.0.8 中有一个 Redis 配置文件 redis.conf，其中一些默认的配置项需要修改（配置项较多，本文仅为举例，修改一些必要的配置）。以下仅以6379端口为例进行配置，6380，6381等端口配置操作类似。将修改后的配置文件分别放入6379~6384文件夹中。

![Redis-cluster十](https://github.com/DemoTransfer/RedisGuide/blob/master/document/picture/Redis-cluster%E5%8D%81.png)

(3) 创建必要启停脚本

逐一手动拉起 Redis 进程较为麻烦，在此，我们可以编写简单的启停脚本完成 redis-server 进程的启停（start.sh 和 stop.sh）。

![Redis-cluster十一]()

（4）简单测试

至此，我们已经完成 Redis 集群创建的前期准备工作，在创建集群之前，我们可以简单测试一下，redis-sever 进程是否可以正常拉起。运行 start.sh 脚本，查看 redis-server 进程如下：

![Redis-cluster十二](https://github.com/DemoTransfer/RedisGuide/blob/master/document/picture/Redis-cluster%E5%8D%81%E4%BA%8C.png)

登陆其中一个 Redis 实例的客户端（以6379为例），查看集群状态：很明显，以节点6379的视角来看，集群处于 fail 状态，clusterknownnodes:1 表示集群中只有一个节点。

![Redis-cluster十三](https://github.com/DemoTransfer/RedisGuide/blob/master/document/picture/Redis-cluster%E5%8D%81%E4%B8%89.png)

<h3>基于 Lettuce 创建 Redis 集群</h3>

关于创建 Redis 集群，官方提供了一个 Ruby 编写的运维软件 redis-trib.rb，使用简单的命令便可以完成创建集群、添加节点、负载均衡等操作。正因为简单，用户很难通过黑盒表现理解其中细节，鉴于此，本文将基于 Lettuce 编写创建 Redis 集群的代码，让读者对 Redis 集群创建有一个更深入的理解。

Redis 发展至今，其对应的开源客户端几乎涵盖所有语言，详情请见<a href="https://redis.io/clients">官网</a>，本文采用 Java 语言开发的 Lettuce 作为 Redis 客户端。Lettuce 是一个可伸缩线程安全的 Redis 客户端，多个线程可以共享同一个 RedisConnection。它利用优秀 Netty NIO 框架来高效地管理多个连接。

![Redis-cluster十四](https://github.com/DemoTransfer/RedisGuide/blob/master/document/picture/Redis-cluster%E5%8D%81%E5%9B%9B.png)

<h3>Redis 集群创建的步骤</h3>

(1) 相互感知，初步形成集群

在上文中，我们已经成功拉起了6个 redis-server 进程，每个进程视为一个节点，这些节点仍处于孤立状态，它们相互之间无法感知对方的存在，既然要创建集群，首先需要让这些孤立的节点相互感知，形成一个集群；

(2) 分配 Slot 给期望的主节点

形成集群之后，仍然无法提供服务，Redis 集群模式下，数据存储于16384个 Slot 中，我们需要将这些 Slot 指派给期望的主节点。何为期望呢？我们有6个节点，3主3备，我们只能将 Slot 指派给3个主节点，至于哪些节点为主节点，我们可以自定义。

(3) 设置从节点

Slot 分配完成后，被分配 Slot 的节点将成为真正可用的主节点，剩下的没有分到 Slot 的节点，即便状态标志为 Master，实际上也不能提供服务。接下来，处于可靠性的考量，我们需要将这些没有被指派 Slot 的节点指定为可用主节点的从节点（Slave）。

经过上述三个步骤，一个精简的3主3备 Redis 集群就搭建完成了。

<h3>基于 Lettuce 的创建集群代码</h3>

根据上述步骤，基于 Lettuce 创建集群的代码如下（仅供入门参考）：

```java
private static void createCluster() throws InterruptedException
    {
        //初始化集群节点列表，并指定主节点列表和从节点列表
        List<ClusterNode> clusterNodeList = new ArrayList<ClusterNode>();
        List<ClusterNode> masterNodeList = new ArrayList<ClusterNode>();
        List<ClusterNode> slaveNodeList = new ArrayList<ClusterNode>();
        String[] endpoints = {"127.0.0.1:6379","127.0.0.1:6380","127.0.0.1:6381"
                ,"127.0.0.1:6382","127.0.0.1:6383","127.0.0.1:6384"};
        int index = 0;
        for (String endpoint : endpoints)
        {
            String[] ipAndPort = endpoint.split(":");
            ClusterNode node = new ClusterNode(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
            clusterNodeList.add(node);
            //将6379，6380，6381设置为主节点，其余为从节点
            if (index < 3)
            {
                masterNodeList.add(node);
            }
            else
            {
                slaveNodeList.add(node);
            }
            index++;
        }       
        //分别与各个Redis节点建立通信连接
        for (ClusterNode node : clusterNodeList)
        {
            RedisURI redisUri = RedisURI.Builder.redis(node.getHost(), node.getPort()).build();
            RedisClient redisClient = RedisClient.create(redisUri);
            try
            {
                StatefulRedisConnection<String, String> connection = redisClient.connect();
                connection.setTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS);
                node.setClient(redisClient);
                node.setConnection(connection);
                node.setMyId(connection.sync().clusterMyId());
            } catch (RedisException e)
            {
                System.out.println("connection failed-->" + node.getHost() + ":" + node.getPort());
            }
        }   

        //执行cluster meet命令是各个孤立的节点相互感知，初步形成集群。
        //只需以一个节点为基准，让所有节点与之meet即可
        ClusterNode firstNode = null;
        for (ClusterNode node : clusterNodeList)
        {
            if (firstNode == null)
            {
                firstNode = node;
            } 
            else
            {
                try
                {
                    node.getConnection().sync().clusterMeet(firstNode.getHost(), firstNode.getPort());
                } 
                catch (RedisCommandTimeoutException | RedisConnectionException e)
                {
                    System.out.println("meet failed-->" + node.getHost() + ":" + node.getPort());
                } 
            }
        }
        //为主节点指派slot,将16384个slot分成三份：5461，5461，5462
        int[] slots = {0,5460,5461,10921,10922,16383};
        index = 0;
        for (ClusterNode node : masterNodeList)
        {
            node.setSlotsBegin(slots[index]);
            index++;
            node.setSlotsEnd(slots[index]);
            index++;
        }
        //通过与各个主节点的连接，执行addSlots命令为主节点指派slot
        System.out.println("Start to set slots...");
        for (ClusterNode node : masterNodeList)
        {
            try
            {
                node.getConnection().sync().clusterAddSlots(createSlots(node.slotsBegin, node.slotsEnd));
            } 
            catch (RedisCommandTimeoutException | RedisConnectionException e)
            {
                System.out.println("add slots failed-->" + node.getHost() + ":" + node.getPort());
            }
        }
        //延时5s，等待slot指派完成
        sleep(5000);
        //为已经指派slot的主节点设置从节点,6379,6380,6381分别对应6382，6383，6384
        index = 0;
        for (ClusterNode node : slaveNodeList)
        {
            try
            {       
                node.getConnection().sync().clusterReplicate(masterNodeList.get(index).getMyId());      
            } 
            catch (RedisCommandTimeoutException | RedisConnectionException e)
            {
                System.out.println("replicate failed-->" + node.getHost() + ":" + node.getPort());
            } 
        }
        //关闭连接
        for (ClusterNode node : clusterNodeList)
        {
            node.getConnection().close();
            node.getClient().shutdown();
        }
    }
```

节点定义代码

```java
public static class ClusterNode
    {
        private String host;
        private int port;
        private int slotsBegin;
        private int slotsEnd;
        private String myId;
        private String masterId;
        private StatefulRedisConnection<String, String> connection;
        private RedisClient redisClient;

        public ClusterNode(String host, int port)
        {
            this.host = host;
            this.port = port;
            this.slotsBegin = 0;
            this.slotsEnd = 0;
            this.myId = null;
            this.masterId = null;
        }

        public String getHost()
        {
            return host;
        }

        public int getPort()
        {
            return port;
        }

        public void setMaster(String masterId)
        {
            this.masterId = masterId;
        }

        public String getMaster()
        {
            return masterId;
        }

        public void setMyId(String myId)
        {
            this.myId = myId;
        }

        public String getMyId()
        {
            return myId;
        }

        public void setSlotsBegin(int first)
        {
            this.slotsBegin = first;
        }

        public void setSlotsEnd(int last)
        {
            this.slotsEnd = last;
        }

        public int getSlotsBegin()
        {
            return slotsBegin;
        }

        public int getSlotsEnd()
        {
            return slotsEnd;
        }

        public void setConnection(StatefulRedisConnection<String, String> connection)
        {
            this.connection = connection;
        }

        public void setClient(RedisClient client)
        {
            this.redisClient = client;
        }

        public StatefulRedisConnection<String, String> getConnection()
        {
            return connection;
        }

        public RedisClient getClient()
        {
            return redisClient;
        }

    }
```

运行上述代码创建集群，再次登陆其中一个节点的客户端（以6379为例），通过命令：cluster nodes，cluster info 查看集群状态信息如下，集群已经处于可用状态。

![Redis-cluster十五]()

经过上述步骤，一个可用的 Redis 集群已经创建完毕，接下来，通过一段代码进程测试：

```java
public static void main(String[] args)
    {
        List<ClusterNode> clusterNodeList = new ArrayList<ClusterNode>();
        List<RedisURI> redisUriList = new ArrayList<RedisURI>();
        String[] endpoints = {"127.0.0.1:6379","127.0.0.1:6380","127.0.0.1:6381"
                ,"127.0.0.1:6382","127.0.0.1:6383","127.0.0.1:6384"};
        for (String endpoint : endpoints)
        {
            String[] ipAndPort = endpoint.split(":");
            ClusterNode node = new ClusterNode(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
            clusterNodeList.add(node);
        }       
        //创建RedisURI
        for (ClusterNode node : clusterNodeList)
        {
            RedisURI redisUri = RedisURI.Builder.redis(node.getHost(), node.getPort()).build();
            redisUriList.add(redisUri);
        }   
        //创建Redis集群客户端，建立连接，执行set，get基本操作
        RedisClusterClient redisClusterClient = RedisClusterClient.create(redisUriList);
        StatefulRedisClusterConnection<String, String> conn = redisClusterClient.connect();
        RedisAdvancedClusterCommands<String, String> cmd = null;
        cmd = conn.sync();
        System.out.println(cmd.set("key-test", "value-test"));
        System.out.println(cmd.get("key-test"));
        //关闭连接
        cmd.close();
        conn.close();
        redisClusterClient.shutdown();      
    }
```

八、Redis“踩坑”
====

在使用 Redis 的过程中，踩过不少坑，本章将列举其中5个，限于篇幅，将在下一场 chat 中介绍相关内容。

<h3>1、'GLIBC_2.14' not found</h3>

**现象**：在 Linux 机 A 上编译 Redis 源码生成 redis-server，在目标 Linux 机 B 上运行 redis-server 时报错：libc.so.6:version `GLIBC_2.14' not found。

**原因**：很明显，报错显示目标 Linux 机 B 上找不到 GLIBC2.14 版本，而 redis-server 编译时使用了 GLIBC2.14。通过命令 ```strings /lib64/libc.so.6 | grep GLIBC``` 可以查看 Linux 操作系统支持的 GLIBC 版本，以便确认问题（libc.so.6路径因操作系统而异）。

进一步，通过命令 ```objdump -T redis-server | fgrep GLIBC_2.14``` 查看 redis-server 依赖的 GLIBC_2.14 版本库的具体函数，截至目前，只发现一个问题函数 memcpy。

**解决方案**：

1. 确保编译机和目标机的 GLIBC 版本兼容；
2. 在涉及问题函数的 Redis 源码中添加约束，显示指定问题函数依赖的 GLIBC 版本，约束代码如下，指定 memcpy 函数依赖的 GLIBC 版本为2.2.5。
```java
__asm__(“.symver memcpy,memcpy@GLIBC_2.2.5”)
```

<h3>2、openssl版本问题</h3>

**现象**：在 Linux 机上拉起 redis-server 进程失败，无报错信息，无日志。

**原因**：通过 GDB 调试，发现问题在于 Linux 系统上 OpenSSL 版本过低，低于 1.0.1e。

**解决方案**：为了解除 Redis 对 OpenSSL 版本的依赖，我们可以修改编译文件（makefile），将依赖的 OpenSSL 库打入编译生成的 redis-server中，一劳永逸。

<h3>3、Lettuce 客户端与集群连接失败</h3>

**现象**：出于安全考量，客户端 Lettuce 访问 Redis 服务端采用了双向证书认证机制，重装操作系统后，Lettuce 客户端与 Redis 服务端连接失败，报错信息价值有限，难以定位根因。

**原因**：经过排查，发现重装操作系统后系统时间设置错误，比北京时间前移10年，超过了证书的有效期，导致认证失败。这种问题报错信息较少，原因简单，有时却难以定位。

<h3>4、Redis 主节点 Slot 丢失问题</h3>

**现象**：反复重启 redis-server 进程，偶现 Master 节点 Slot 丢失的情况，Redis 集群不可用，查看集群信息如下（举例）：Master 节点6381的 Slot 丢失，造成 Redis 集群 Slot 不满，不能提供服务。问题根因及解决方案将在下一篇文章中呈现。

<h3>5、ERR Slot xxx is already busy</h3>

**现象**： Redis 集群故障转移（failover）时，从机升主接管 Slot 时报错：ERR Slot xxx is already busy，导致故障转移失败。问题根因及解决方案将在下一场 Chat 中呈现。
