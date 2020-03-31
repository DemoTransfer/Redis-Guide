RedisGuide
=====

推荐阅读
------

* <a href="https://github.com/doocs/advanced-java">缓存</a>

* <a href="https://redis.io/">Redis官网</a>

* <a href="http://www.redis.cn/">Redis中文官网</a>

* <a href="http://www.linuxe.cn/sort/redis">Redis运维教程</a>

Redis部署：基于腾讯云Linux服务器安装Redis服务
------
   
    
目录
====

工具准备
------

* 安装Redis的Linux服务器

* idea编译器

基本知识扫盲
------

* <a href="https://www.cnblogs.com/kismetv/category/1186633.html">Redis内存模型、持久化、主从复制、哨兵、集群介绍</a>

    * <a href="https://www.cnblogs.com/kismetv/p/8654978.html">Redis内存模型</a>
    
    * <a href="https://www.cnblogs.com/kismetv/p/9137897.html">Redis持久化</a>
    
    * <a href="https://www.cnblogs.com/kismetv/p/9236731.html">Redis主从复制</a>
    
    * <a href="https://www.cnblogs.com/kismetv/p/9609938.html">Redis哨兵</a>
    
    * <a href="https://www.cnblogs.com/kismetv/p/9853040.html">Redis集群</a>

* <a href="https://github.com/DemoTransfer/RedisGuide/blob/master/document/basic/why-cache.md">在项目中缓存是如何使用的？缓存如果使用不当会造成什么后果？</a>

* <a href="https://github.com/DemoTransfer/RedisGuide/blob/master/document/basic/redis-single-thread-model.md">Redis 和 Memcached 有什么区别？Redis 的线程模型是什么？为什么单线程的 Redis 比多线程的 Memcached 效率要高得多？</a>

* <a href="https://github.com/DemoTransfer/RedisGuide/blob/master/document/basic/redis-data-types.md">Redis 都有哪些数据类型？分别在哪些场景下使用比较合适？</a>

* <a href="https://github.com/DemoTransfer/RedisGuide/blob/master/document/basic/redis-expiration-policies-and-lru.md">Redis 的过期策略都有哪些？手写一下 LRU 代码实现？</a>

* <a href="https://github.com/DemoTransfer/RedisGuide/blob/master/document/basic/how-to-ensure-high-concurrency-and-high-availability-of-redis.md">如何保证 Redis 高并发、高可用？Redis 的主从复制原理能介绍一下么？Redis 的哨兵原理能介绍一下么？</a>

* <a href="https://github.com/DemoTransfer/RedisGuide/blob/master/document/basic/redis-persistence.md">Redis 的持久化有哪几种方式？不同的持久化机制都有什么优缺点？持久化机制具体底层是如何实现的？</a>

* <a href="https://github.com/DemoTransfer/RedisGuide/blob/master/document/basic/redis-cluster.md">Redis 集群模式的工作原理能说一下么？在集群模式下，Redis 的 key 是如何寻址的？分布式寻址都有哪些算法？了解一致性 hash 算法吗？如何动态增加和删除一个节点？</a>

* <a href="https://github.com/DemoTransfer/RedisGuide/blob/master/document/basic/redis-caching-avalanche-and-caching-penetration.md">了解什么是 redis 的雪崩、穿透和击穿？Redis 崩溃之后会怎么样？系统该如何应对这种情况？如何处理 Redis 的穿透？</a>

* <a href="https://github.com/DemoTransfer/RedisGuide/blob/master/document/basic/redis-consistence.md">如何保证缓存与数据库的双写一致性？</a>

* <a href="https://github.com/DemoTransfer/RedisGuide/blob/master/document/basic/redis-cas.md">Redis 的并发竞争问题是什么？如何解决这个问题？了解 Redis 事务的 CAS 方案吗？</a>

* <a href="https://github.com/DemoTransfer/RedisGuide/blob/master/document/basic/redis-production-environment.md">生产环境中的 Redis 是怎么部署的？</a>


基本操作Demo
------

* <a href="https://github.com/DemoTransfer/RedisGuide/tree/master/coding/redis-distributed-lock">Redis实现分布式锁示例</a>

* <a href="https://github.com/DemoTransfer/RedisGuide/blob/master/coding/redis-client-java-utils/RedilUtil.md">最全的Java操作Redis的工具类，封装了对Redis五种基本类型的各种操作，力求符合Redis的原生操作，使用StringRedisTemplate实现！</a>

   * <a href="https://github.com/whvcse/RedisUtil">来源于RedisUtil</a>

Redis持久化
------

![Redis持久化](https://github.com/DemoTransfer/Redis-Guide/blob/master/document/redis-guide/redis-persistence/picture/Redis%E6%8C%81%E4%B9%85%E5%8C%96.png)

* <a href="https://github.com/DemoTransfer/Redis-Guide/blob/master/document/redis-guide/redis-persistence/RDB%E5%92%8CAOF%E6%8C%81%E4%B9%85%E5%8C%96%E5%A6%82%E4%BD%95%E9%80%89%E6%8B%A9.md">RDB和AOF持久化如何选择</a>

Redis复制的原理和优化
------

![Redis复制的原理和优化]()

* <a href="https://github.com/DemoTransfer/Redis-Guide/blob/master/document/redis-guide/redis-copy/Redis%E7%9A%84%E4%B8%BB%E4%BB%8E%E5%A4%8D%E5%88%B6.md">Redis的主从复制</a>
