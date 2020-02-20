看在前面
====

> * <a href="http://www.linuxe.cn/post-375.html">Redis教程（九）cluster集群的配置</a>


Redis的sentinel哨兵模式虽然可以解决Redis单点故障的问题，但是一主多从的结构必然会导致客户端写入数据只能在Master节点上操作，当写入量特别大的时候主节点压力就会很大，sentinel无法分布式将数据存放到其他服务器上。Redis从3.0版本之后提供了cluster集群模式，解决了这个问题。但是这里需要泼个冷水，集群分布式不一定好，分布式集群的客户端性能一般会低一些，flush、mget、keys等命令也不能跨节点使用，客户端的维护也更复杂，所以业务能在哨兵下满足需求尽量用哨兵模式。

一、Redis cluster集群简介
====

1、Redis cluster部署条件
------

**至少要有三个主节点才能构成集群**，数据分区规则采用虚拟槽（16384个槽）方式，每个节点负责一部分数据。除了三个主节点意外还需要为他们各自配置一个slave以支持主从切换。Redis cluster采用了分片机制，当有多个节点的时候，其每一个节点都可以作为客户端的入口进行连接并写入数据。Redis cluster的分片机制让每个节点都存放了一部分数据（比如有1W个key分布在了5个cluster节点上，每个节点可能只存储了2000个key， 但是每一个节点都有一个类似index索引的东西记录了所有key的分布情况。），且每一个节点还应该有一个slave节点作为备份节点（比如用3台机器部署成Redis cluster，还应该为这三台Redis做主从部署，所以一共要6台机器），当master节点故障时slave节点会选举提升为master节点。

2、Redis cluster集群什么时候不可用
------

任意master挂掉且该master没有slave节点，集群将进入fail状态。如果master有slave节点，但是有半数以上master挂掉，集群也将进入fail状态。当集群fail时，所有对集群的操作都不可用，会出现clusterdown the cluster is down的错误

3、Redis cluster集群监控端口
------

16379（其实是Redis当前的端口+10000，因为默认是6379，所以这里就成了16379，用于集群节点相互通信）

4、Redis cluster容错与选举机制
------

所有master参与选举，当半数以上的master节点与故障节点通信超时将触发故障转移

二、Redis cluster配置文件
====

redis-cluster的配置信息包含在了redis.conf中，要修改的主要有6个选项（**每一个节点都需要做这些配置**）：

```java
1 cluster-enabled yes  #开启集群

2 cluster-config-file nodes-6379.conf  #集群配置信息文件，由Redis自行更新，不用手动配置。每个节点都有一个集群配置文件用于持久化保存集群信息，需确保与运行中实例的配置文件名    不冲突。

3 cluster-node-timeout 15000  #节点互连超时时间，毫秒为单位

4 cluster-slave-validity-factor 10  #在进行故障转移的时候全部slave都会请求申请为master，但是有些slave可能与master断开连接一段时间了导致数据过于陈旧，不应该被提升为master。该参数就是用来判断slave节点与master断线的时间是否过长。判断方法是：比较slave断开连接的时间和(node-timeout * slave-validity-factor)+ repl-ping-slave-period如果节点超时时间为三十秒, 并且slave-validity-factor为10，假设默认的repl-ping-slave-period是10秒，即如果超过310秒slave将不会尝试进行故障转移

5 cluster-migration-barrier 1  #master的slave数量大于该值，slave才能迁移到其他孤立master上，如这个参数被设为2，那么只有当一个主节点拥有2个可工作的从节点时，它的一个从节点才会尝试迁移。

6 cluster-require-full-coverage yes  #集群所有节点状态为ok才提供服务。建议设置为no，可以在slot没有全部分配的时候提供服务。
```

三、正式启动redis cluster服务
====

首先要让集群正常运作至少需要三个主节点（考虑主从的话还需要额外准备三个从节点）。做实验的话可以用二台机器启动不同的redis端口（比如6379、6380、6381）来模拟不同的机器。

1、每个主从节点均需要启动cluster服务，启动后查看下端口，可以看到端口后面多了cluster这样的信息

```java
redis-server redis.conf
```

![nine-1](https://github.com/DemoTransfer/RedisGuide/blob/master/document/redis-operative/picture/nine-1.png)

2、单单启动cluster后还没有正式组成集群，还需要用到redis-trib.rb命令来创建集群，由于redis-trib.rb命令是一个ruby脚本，会对ruby环境有一些依赖，在执行前需要安装以下软件包（Redis 5开始可以使用redis-cli --cluster来创建集群，命令语法和redis-trib.rb脚本一样，省去了配置ruby环境的步骤）

```java
1 yum install ruby rubygems

2 gem source -l  #安装完成后可以查看当前ruby源

3 gem sources --remove https://rubygems.org/  #去掉官方源

4 gem sources --remove http://rubygems.org/  #去掉官方源

5 gem sources -a https://ruby.taobao.org/  #新增国内源

6 gem install redis --version 3.2.0  #安装redis与ruby连接接口
```

ruby环境配置完毕后，只用在一台机器上执行即可，命令默认位于redis的src目录下

```java
1 redis-trib.rb  create  --replicas 1  192.168.1.101:6379  192.168.1.102:6379   192.168.1.103:6379   192.168.1.104:6379   192.168.1.105:6379   192.168.1.106:6379  

2 #create 创建集群

3 #replicas  代表每个主节点有几个从节点

4 #后面跟上的IP和端口是所有master和slave的节点信息
```

3、执行完毕后会看到如下信息，显示了master和slave的信息等，这些主从是自动分配出来的，通过ID号可以看出对应的主从关系。如果确认没有问题输入“yes”回车确定

![nine-2](https://github.com/DemoTransfer/RedisGuide/blob/master/document/redis-operative/picture/nine-2.png)

4、如果提示有ERR Slot xxxxx is already busy这样的报错说明是有空间被使用掉了，这个时候可以删除cluster-config-file选项所指定的配置文件，然后重新启动Redis后再执行redis-trib.rb的命令。以后要增加集群节点的话可以使用以下命令

```java
redis-trib.rb add-node 192.168.0.110:6379  192.168.0.111:6379  #第一个ip是新节点，第二个ip是已存在的节点
```

四、测试集群工作
====

1、集群一旦搭建好了后必须使用redis-cli -c 选项以集群模式进行操作。集群模式下只有0号数据库可用，无法再通过select来切换数据库。登录后创建一些key用于测试，可以看到输出信息显示这个key是被存到了其他机器上。使用get获取key的时候也可以看到该key是被分配到了哪个节点

![nine-3](https://github.com/DemoTransfer/RedisGuide/blob/master/document/redis-operative/picture/nine-3.png)

2、查看redis cluster节点状态

cluster nodes命令可以看到自己和其他节点的状态，集群模式下有主节点挂掉的话可以在这里观察到切换情况；cluste info命令可以看到集群的详细状态，包括集群状态、分配槽信息

![nine-4](https://github.com/DemoTransfer/RedisGuide/blob/master/document/redis-operative/picture/nine-4.png)

3、手动切换redis cluster的主从关系：

redis cluster发生主从切换后，即使之前的主节点恢复了也不会变回主节点，而是作为从节点在工作，这一点和sentine模式是一样的。如果想要它变回主节点，只需要在该节点执行命令cluster failover
