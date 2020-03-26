看在前面
====

> * <a href="https://juejin.im/post/5dccf260f265da0bf66b626d">《今天面试了吗》-Redis</a>

> 作者：坚持就是胜利
链接：https://juejin.im/post/5dccf260f265da0bf66b626d
来源：掘金
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。

今天，我不自量力的面试了某大厂的java开发岗位，迎面走来一位风尘仆仆的中年男子，手里拿着屏幕还亮着的mac，他冲着我礼貌的笑了笑，然后说了句“不好意思，让你久等了”，然后示意我坐下，说：“我们开始吧。看了你的简历，觉得你对redis应该掌握的不错，我们今天就来讨论下redis......”。我想：“来就来，兵来将挡水来土掩”。

Redis是什么
====

* 面试官：你先来说下redis是什么吧

* 我：（这不就是总结下redis的定义和特点嘛）Redis是C语言开发的一个开源的（遵从BSD协议）高性能键值对（key-value）的内存数据库，可以用作数据库、缓存、消息中间件等。它是一种NoSQL（not-only sql，泛指非关系型数据库）的数据库。

* 我顿了一下，接着说：Redis作为一个内存数据库。

1、性能优秀，数据在内存中，读写速度非常快，支持并发10W QPS；

2、单进程单线程，是线程安全的，采用IO多路复用机制；

3、丰富的数据类型，支持字符串（strings）、散列（hashes）、列表（lists）、集合（sets）、有序集合（sorted sets）等；

4、支持数据持久化。可以将内存中数据保存在磁盘中，重启时加载；

5、主从复制，哨兵，高可用；

6、可以用作分布式锁；

7、可以作为消息中间件使用，支持发布订阅

五种数据类型
====

* 面试官：总结的不错，看来是早有准备啊。刚来听你提到redis支持五种数据类型，那你能简单说下这五种数据类型吗？

* 我：当然可以，但是在说之前，我觉得有必要先来了解下Redis内部内存管理是如何描述这5种数据类型的。说着，我拿着笔给面试官画了一张图：

![Redis-数据结构一]()

* 我：首先redis内部使用一个redisObject对象来表示所有的key和value，redisObject最主要的信息如上图所示：type表示一个value对象具体是何种数据类型，encoding是不同数据类型在redis内部的存储方式。比如：type=string表示value存储的是一个普通字符串，那么encoding可以是raw或者int。

* 我顿了一下，接着说：下面我简单说下5种数据类型：

1. string是redis最基本的类型，可以理解成与memcached一模一样的类型，一个key对应一个value。value不仅是string，也可以是数字。string类型是二进制安全的，意思是redis的string类型可以包含任何数据，比如jpg图片或者序列化的对象。**string类型的值最大能存储512M**。

2. Hash是一个键值（key-value）的集合。redis的hash是一个string的key和value的映射表，Hash特别适合存储对象。**常用命令**：hget,hset,hgetall等。

3. list列表是简单的字符串列表，按照插入顺序排序。可以添加一个元素到列表的头部（左边）或者尾部（右边）  常用命令：lpush、rpush、lpop、rpop、lrange(获取列表片段)等。
应用场景：list应用场景非常多，也是Redis最重要的数据结构之一，比如twitter的关注列表，粉丝列表都可以用list结构来实现。**数据结构**：list就是链表，可以用来当消息队列用。redis提供了List的push和pop操作，还提供了操作某一段的api，可以直接查询或者删除某一段的元素。**实现方式**：redis list的是实现是一个双向链表，既可以支持反向查找和遍历，更方便操作，不过带来了额外的内存开销。

4. set是string类型的无序集合。集合是通过hashtable实现的。set中的元素是没有顺序的，而且是没有重复的。**常用命令**：sdd、spop、smembers、sunion等。**应用场景**：redis set对外提供的功能和list一样是一个列表，特殊之处在于set是自动去重的，而且set提供了判断某个成员是否在一个set集合中。

5. zset和set一样是string类型元素的集合，且不允许重复的元素。常用命令：zadd、zrange、zrem、zcard等。**使用场景**：sorted set可以通过用户额外提供一个优先级（score）的参数来为成员排序，并且是插入有序的，即自动排序。当你需要一个有序的并且不重复的集合列表，那么可以选择sorted set结构。和set相比，sorted set关联了一个double类型权重的参数score，使得集合中的元素能够按照score进行有序排列，redis正是通过分数来为集合中的成员进行从小到大的排序。**实现方式**：Redis sorted set的内部使用HashMap和跳跃表(skipList)来保证数据的存储和有序，HashMap里放的是成员到score的映射，而跳跃表里存放的是所有的成员，排序依据是HashMap里存的score，使用跳跃表的结构可以获得比较高的查找效率，并且在实现上比较简单。

数据类型应用场景总结
------

![Redis面试二]()

* 面试官：想不到你平时也下了不少工夫，那redis缓存你一定用过的吧

* 我：用过的。。

* 面试官：那你跟我说下你是怎么用的？

* 我是结合spring boot使用的。一般有两种方式，一种是直接通过RedisTemplate来使用，另一种是使用spring cache集成Redis（也就是注解的方式）。具体的代码我就不说了，在我的掘金中有一个demo（见下）。

Redis缓存
====

* 直接通过RedisTemplate来使用

* 使用spring cache集成Redis pom.xml中加入以下依赖：

```java
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-pool2</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.session</groupId>
        <artifactId>spring-session-data-redis</artifactId>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

* spring-boot-starter-data-redis:在spring boot 2.x以后底层不再使用Jedis，而是换成了Lettuce。

* commons-pool2：用作redis连接池，如不引入启动会报错

* spring-session-data-redis：spring session引入，用作共享session。 配置文件application.yml的配置：

```java
server:
  port: 8082
  servlet:
    session:
      timeout: 30ms
spring:
  cache:
    type: redis
  redis:
    host: 127.0.0.1
    port: 6379
    password:
    # redis默认情况下有16个分片，这里配置具体使用的分片，默认为0
    database: 0
    lettuce:
      pool:
        # 连接池最大连接数(使用负数表示没有限制),默认8
        max-active: 100
```

创建实体类User.java

```java
public class User implements Serializable{

    private static final long serialVersionUID = 662692455422902539L;

    private Integer id;

    private String name;

    private Integer age;

    public User() {
    }

    public User(Integer id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
```

RedisTemplate的使用方式
------

默认情况下的模板只能支持RedisTemplate<String, String>，也就是只能存入字符串，所以自定义模板很有必要。添加配置类RedisCacheConfig.java

