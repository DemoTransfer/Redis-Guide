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

```java
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisCacheConfig {

    @Bean
    public RedisTemplate<String, Serializable> redisCacheTemplate(LettuceConnectionFactory connectionFactory) {

        RedisTemplate<String, Serializable> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setConnectionFactory(connectionFactory);
        return template;
    }
}
```

测试类

```java
@RestController
@RequestMapping("/user")
public class UserController {

    public static Logger logger = LogManager.getLogger(UserController.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Serializable> redisCacheTemplate;

    @RequestMapping("/test")
    public void test() {
        redisCacheTemplate.opsForValue().set("userkey", new User(1, "张三", 25));
        User user = (User) redisCacheTemplate.opsForValue().get("userkey");
        logger.info("当前获取对象：{}", user.toString());
    }
    
```

使用spring cache集成redis
------

spring cache具备很好的灵活性，不仅能够使用SPEL(spring expression language)来定义缓存的key和各种condition，还提供了开箱即用的缓存临时存储方案，也支持和主流的专业缓存如EhCache、Redis、Guava的集成。

定义接口UserService.java

```java
public interface UserService {

    User save(User user);

    void delete(int id);

    User get(Integer id);
}
```

接口实现类UserServiceImpl.java

```java
@Service
public class UserServiceImpl implements UserService{

    public static Logger logger = LogManager.getLogger(UserServiceImpl.class);

    private static Map<Integer, User> userMap = new HashMap<>();
    static {
        userMap.put(1, new User(1, "肖战", 25));
        userMap.put(2, new User(2, "王一博", 26));
        userMap.put(3, new User(3, "杨紫", 24));
    }


    @CachePut(value ="user", key = "#user.id")
    @Override
    public User save(User user) {
        userMap.put(user.getId(), user);
        logger.info("进入save方法，当前存储对象：{}", user.toString());
        return user;
    }

    @CacheEvict(value="user", key = "#id")
    @Override
    public void delete(int id) {
        userMap.remove(id);
        logger.info("进入delete方法，删除成功");
    }

    @Cacheable(value = "user", key = "#id")
    @Override
    public User get(Integer id) {
        logger.info("进入get方法，当前获取对象：{}", userMap.get(id)==null?null:userMap.get(id).toString());
        return userMap.get(id);
    }
}
```

为了方便演示数据库的操作，这里直接定义了一个Map<Integer,User> userMap，这里的核心是三个注解@Cachable、@CachePut和@CacheEvict。

测试类：UserController

```java
@RestController
@RequestMapping("/user")
public class UserController {

    public static Logger logger = LogManager.getLogger(UserController.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Serializable> redisCacheTemplate;

    @Autowired
    private UserService userService;

    @RequestMapping("/test")
    public void test() {
        redisCacheTemplate.opsForValue().set("userkey", new User(1, "张三", 25));
        User user = (User) redisCacheTemplate.opsForValue().get("userkey");
        logger.info("当前获取对象：{}", user.toString());
    }


    @RequestMapping("/add")
    public void add() {
        User user = userService.save(new User(4, "李现", 30));
        logger.info("添加的用户信息：{}",user.toString());
    }

    @RequestMapping("/delete")
    public void delete() {
        userService.delete(4);
    }

    @RequestMapping("/get/{id}")
    public void get(@PathVariable("id") String idStr) throws Exception{
        if (StringUtils.isBlank(idStr)) {
            throw new Exception("id为空");
        }
        Integer id = Integer.parseInt(idStr);
        User user = userService.get(id);
        logger.info("获取的用户信息：{}",user.toString());
    }
}
```

用缓存要注意，启动类要加上一个注解开启缓存

```java
@SpringBootApplication(exclude=DataSourceAutoConfiguration.class)
@EnableCaching
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
```

缓存注解
------

1. @Cacheable 根据方法的请求参数对其结果进行缓存

* key：缓存的key，可以为空，如果指定要按照SPEL表达式编写，如果不指定，则按照方法的所有参数进行组合。

* value：缓存的名称，必须指定至少一个（如 ```@Cacheable (value='user')```或者```@Cacheable(value={'user1','user2'})）```

* condition：缓存的条件，可以为空，使用SPEL编写，返回true或者false，只有为true才进行缓存。

2. @CachePut

根据方法的请求参数对其结果进行缓存，和@Cacheable不同的是，它每次都会触发真实方法的调用。参数描述见上。

3. @CacheEvict

* 根据条件对缓存进行清空

* key：同上

* value：同上

* condition：同上

* allEntries：是否清空所有缓存内容，缺省为false，如果指定为true，则方法调用后将立即清空所有缓存

* beforeInvocation：是否在方法执行前就清空，缺省为false，如果指定为true，则在方法还没有执行的时候就清空缓存。缺省情况下，如果方法执行抛出异常，则不会清空缓存。

缓存问题
====

* 面试官：看了一下你的demo，简单易懂。那你在实际项目中使用缓存有遇到什么问题或者会遇到什么问题你知道吗？

* 我：缓存和数据库数据一致性问题：分布式环境下非常容易出现缓存和数据库间数据一致性问题，针对这一点，如果项目对缓存的要求是强一致性的，那么就不要使用缓存。我们只能采取合适的策略来降低缓存和数据库间数据不一致的概率，而无法保证两者间的强一致性。合适的策略包括合适的缓存更新策略，更新数据库后及时更新缓存、缓存失败时增加重试机制。

* 面试官：Redis雪崩了解吗？

* 我：我了解的，目前电商首页以及热点数据都会去做缓存，一般缓存都是定时任务去刷新，或者查不到之后去更新缓存的，定时任务刷新就有一个问题。举个栗子：如果首页所有Key的失效时间都是12小时，中午12点刷新的，我零点有个大促活动大量用户涌入，假设每秒6000个请求，本来缓存可以抗住每秒5000个请求，但是缓存中所有Key都失效了。此时6000个/秒的请求全部落在了数据库上，数据库必然扛不住，真实情况可能DBA都没反应过来直接挂了，此时，如果没什么特别的方案来处理，DBA很着急，重启数据库，但是数据库立马又被新流量给打死了。这就是我理解的缓存雪崩。

* 我心想：同一时间大面积失效，瞬间Redis跟没有一样，那这个数量级别的请求直接打到数据库几乎是灾难性的，你想想如果挂的是一个用户服务的库，那其他依赖他的库所有接口几乎都会报错，如果没做熔断等策略基本上就是瞬间挂一片的节奏，你怎么重启用户都会把你打挂，等你重启好的时候，用户早睡觉去了，临睡之前，骂骂咧咧“什么垃圾产品”。

* 面试官摸摸了自己的头发：嗯，还不错，那这种情况你都是怎么应对的？

* 我：处理缓存雪崩简单，在批量往Redis存数据的时候，把每个Key的失效时间都加个随机值就好了，这样可以保证数据不会再同一时间大面积失效。

```java
setRedis（key, value, time+Math.random()*10000）;
```

如果Redis是集群部署，将热点数据均匀分布在不同的Redis库中也能避免全部失效。或者设置热点数据永不过期，有更新操作就更新缓存就好了（比如运维更新了首页商品，那你刷下缓存就好了，不要设置过期时间），电商首页的数据也可以用这个操作，保险。

* 面试官：那你了解缓存穿透和击穿么，可以说说他们跟雪崩的区别吗？

* 我：嗯，了解，先说下缓存穿透吧，缓存穿透是指缓存和数据库中都没有的数据，而用户（黑客）不断发起请求，举个栗子：我们数据库的id都是从1自增的，如果发起id=-1的数据或者id特别大不存在的数据，这样的不断攻击导致数据库压力很大，严重会击垮数据库。

* 我又接着说：至于缓存击穿嘛，这个跟缓存雪崩有点像，但是又有一点不一样，缓存雪崩是因为大面积的缓存失效，打崩了DB，而缓存击穿不同的是缓存击穿是指一个Key非常热点，在不停地扛着大量的请求，大并发集中对这一个点进行访问，当这个Key在失效的瞬间，持续的大并发直接落到了数据库上，就在这个Key的点上击穿了缓存。

* 面试官露出欣慰的眼光：那他们分别怎么解决？

* 我：缓存穿透我会在接口层增加校验，比如用户鉴权，参数做校验，不合法的校验直接return，比如id做基础校验，id<=0直接拦截。

* 面试官：那你还有别的方法吗？

* 我：我记得Redis里还有一个高级用法**布隆过滤器（Bloom Filter）**这个也能很好的预防缓存穿透的发生，他的原理也很简单，就是利用高效的数据结构和算法快速判断出你这个Key是否在数据库中存在，不存在你return就好了，存在你就去查DB刷新KV再return。缓存击穿的话，设置热点数据永不过期，或者加上互斥锁就搞定了。作为暖男，代码给你准备好了，拿走不谢。

```java
public static String getData(String key) throws InterruptedException {
	//从Redis查询数据
	String result = getDataByKV(key);
	//参数校验
	if (StringUtils.isBlank(result)) {
		try {
			//获得锁
			if (reenLock.tryLock()) {
				//去数据库查询
				result = getDataByDB(key);
				//校验
				if (StringUtils.isNotBlank(result)) {
					//插进缓存
					setDataToKV(key, result);
				}
			} else {
				//睡一会再拿
				Thread.sleep(100L);
				result = getData(key);
			}
		} finally {
			//释放锁
			reenLock.unlock();
		}
	}
	return result;
}
```

Redis为何这么快
====

* 面试官：redis作为缓存大家都在用，那redis一定很快咯？

* 我：当然了，官方提供的数据可以达到100000+的QPS（每秒内的查询次数），这个数据不比Memcached差！

* 面试官：redis这么快，它的“多线程模型”你了解吗？（露出邪魅一笑）

* 我：您是想问Redis这么快，为什么还是单线程的吧。Redis确实是单进程单线程的模型，因为Redis完全是基于内存的操作，CPU不是Redis的瓶颈，Redis的瓶颈最有可能是机器内存的大小或者网络带宽。既然单线程容易实现，而且CPU不会成为瓶颈，那就顺理成章的采用单线程的方案了（毕竟采用多线程会有很多麻烦）。

* 面试官：嗯，是的。那你能说说Redis是单线程的，为什么还能这么快吗？

* 我：可以这么说吧。第一：Redis完全基于内存，绝大部分请求是纯粹的内存操作，非常迅速，数据存在内存中，类似于HashMap，HashMap的优势就是查找和操作的时间复杂度是O(1)。第二：数据结构简单，对数据操作也简单。第三：采用单线程，避免了不必要的上下文切换和竞争条件，不存在多线程导致的CPU切换，不用去考虑各种锁的问题，不存在加锁释放锁操作，没有死锁问题导致的性能消耗。第四：使用多路复用IO模型，非阻塞IO。

Redis和Memcached的区别
====

* 面试官：嗯嗯，说的很详细。那你为什么选择Redis的缓存方案而不用memcached呢

* 我：1、存储方式上：memcache会把数据全部存在内存之中，断电后会挂掉，数据不能超过内存大小。redis有部分数据存在硬盘上，这样能保证数据的持久性。2、数据支持类型上：memcache对数据类型的支持简单，只支持简单的key-value，，而redis支持五种数据类型。3、使用底层模型不同：它们之间底层实现方式以及与客户端之间通信的应用协议不一样。redis直接自己构建了VM机制，因为一般的系统调用系统函数的话，会浪费一定的时间去移动和请求。4、value的大小：redis可以达到1GB，而memcache只有1MB。

淘汰策略
====

* 面试官：那你说说你知道的redis的淘汰策略有哪些？

* 我：Redis有六种淘汰策略

![Redis面试三]()

补充一下：Redis4.0加入了LFU(least frequency use)淘汰策略，包括volatile-lfu和allkeys-lfu，通过统计访问频率，将访问频率最少，即最不经常使用的KV淘汰。

持久化
====

* 面试官：你对redis的持久化机制了解吗？能讲一下吗？

* 我：redis为了保证效率，数据缓存在了内存中，但是会周期性的把更新的数据写入磁盘或者把修改操作写入追加的记录文件中，以保证数据的持久化。Redis的持久化策略有两种：
1、RDB：快照形式是直接把内存中的数据保存到一个dump的文件中，定时保存，保存策略。
2、AOF：把所有的对Redis的服务器进行修改的命令都存到一个文件里，命令的集合。Redis默认是快照RDB的持久化方式。
当Redis重启的时候，它会优先使用AOF文件来还原数据集，因为AOF文件保存的数据集通常比RDB文件所保存的数据集更完整。你甚至可以关闭持久化功能，让数据只在服务器运行时存。

* 面试官：那你再说下RDB是怎么工作的？

* 我：默认Redis是会以快照"RDB"的形式将数据持久化到磁盘的一个二进制文件dump.rdb。工作原理简单说一下：当Redis需要做持久化时，Redis会fork一个子进程，子进程将数据写到磁盘上一个临时RDB文件中。当子进程完成写临时文件后，将原来的RDB替换掉，这样的好处是可以copy-on-write。

* 我：RDB的优点是：这种文件非常适合用于备份：比如，你可以在最近的24小时内，每小时备份一次，并且在每个月的每一天也备份一个RDB文件。这样的话，即使遇上问题，也可以随时将数据集还原到不同的版本。RDB非常适合灾难恢复。RDB的缺点是：如果你需要尽量避免在服务器故障时丢失数据，那么RDB不合适你。

* 面试官：那你要不再说下AOF？？

* 我：（说就一起说下吧）使用AOF做持久化，每一个写命令都通过write函数追加到appendonly.aof中，配置方式如下：

```java
appendfsync yes   
appendfsync always     #每次有数据修改发生时都会写入AOF文件。
appendfsync everysec   #每秒钟同步一次，该策略为AOF的缺省策略。
```

AOF可以做到全程持久化，只需要在配置中开启 appendonly yes。这样redis每执行一个修改数据的命令，都会把它添加到AOF文件中，当redis重启时，将会读取AOF文件进行重放，恢复到redis关闭前的最后时刻。

* 我顿了一下，继续说：使用AOF的优点是会让redis变得非常耐久。可以设置不同的fsync策略，aof的默认策略是每秒钟fsync一次，在这种配置下，就算发生故障停机，也最多丢失一秒钟的数据。缺点是对于相同的数据集来说，AOF的文件体积通常要大于RDB文件的体积。根据所使用的fsync策略，AOF的速度可能会慢于RDB。

* 面试官又问：你说了这么多，那我该用哪一个呢？

* 我：如果你非常关心你的数据，但仍然可以承受数分钟内的数据丢失，那么可以额只使用RDB持久。AOF将Redis执行的每一条命令追加到磁盘中，处理巨大的写入会降低Redis的性能，不知道你是否可以接受。数据库备份和灾难恢复：定时生成RDB快照非常便于进行数据库备份，并且RDB恢复数据集的速度也要比AOF恢复的速度快。当然了，redis支持同时开启RDB和AOF，系统重启后，redis会优先使用AOF来恢复数据，这样丢失的数据会最少。

主从复制
====

* 面试官：redis单节点存在单点故障问题，为了解决单点问题，一般都需要对redis配置从节点，然后使用哨兵来监听主节点的存活状态，如果主节点挂掉，从节点能继续提供缓存功能，你能说说redis主从复制的过程和原理吗？

* 我有点懵，这个说来就话长了。但幸好提前准备了：主从配置结合哨兵模式能解决单点故障问题，提高redis可用性。从节点仅提供读操作，主节点提供写操作。对于读多写少的状况，可给主节点配置多个从节点，从而提高响应效率。

* 我顿了一下，接着说：关于复制过程，是这样的：

1. 从节点执行slaveof[masterIP][masterPort]，保存主节点信息

2. 从节点中的定时任务发现主节点信息，建立和主节点的socket连接

3. 从节点发送Ping信号，主节点返回Pong，两边能互相通信

4. 连接建立后，主节点将所有数据发送给从节点（数据同步）

5. 主节点把当前的数据同步给从节点后，便完成了复制的建立过程。接下来，主节点就会持续的把写命令发送给从节点，保证主从数据一致性。

* 面试官：那你能详细说下数据同步的过程吗？

* （我心想：这也问的太细了吧）我：可以。redis2.8之前使用sync[runId][offset]同步命令，redis2.8之后使用psync[runId][offset]命令。两者不同在于，sync命令仅支持全量复制过程，psync支持全量和部分复制。介绍同步之前，先介绍几个概念：

runId：每个redis节点启动都会生成唯一的uuid，每次redis重启后，runId都会发生变化。

offset：主节点和从节点都各自维护自己的主从复制偏移量offset，当主节点有写入命令时，offset=offset+命令的字节长度。从节点在收到主节点发送的命令后，也会增加自己的offset，并把自己的offset发送给主节点。这样，主节点同时保存自己的offset和从节点的offset，通过对比offset来判断主从节点数据是否一致。

repl_backlog_size：保存在主节点上的一个固定长度的先进先出队列，默认大小是1MB。

1) 主节点发送数据给从节点过程中，主节点还会进行一些写操作，这时候的数据存储在复制缓冲区中。从节点同步主节点数据完成后，主节点将缓冲区的数据继续发送给从节点，用于部分复制。

2) 主节点响应写命令时，不但会把命名发送给从节点，还会写入复制积压缓冲区，用于复制命令丢失的数据补救。

![Redis面试四]()

上面是psync的执行流程：

从节点发送psync[runId][offset]命令，主节点有三种响应：

1) FULLRESYNC：第一次连接，进行全量复制

2) CONTINUE：进行部分复制

3) ERR：不支持psync命令，进行全量复制

* 面试官：很好，那你能具体说下全量复制和部分复制的过程吗？

* 我：可以

![Redis面试五]()

上面是全量复制的流程。主要有以下几步：

1) 从节点发送psync ? -1命令（因为第一次发送，不知道主节点的runId，所以为?，因为是第一次复制，所以offset=-1）。

2) 主节点发现从节点是第一次复制，返回FULLRESYNC {runId} {offset}，runId是主节点的runId，offset是主节点目前的offset。

3) 从节点接收主节点信息后，保存到info中。

4) 主节点在发送FULLRESYNC后，启动bgsave命令，生成RDB文件（数据持久化）。

5) 主节点发送RDB文件给从节点。到从节点加载数据完成这段期间主节点的写命令放入缓冲区。

6) 从节点清理自己的数据库数据。

7) 从节点加载RDB文件，将数据保存到自己的数据库中。

8) 如果从节点开启了AOF，从节点会异步重写AOF文件。

关于部分复制有以下几点说明：

1. 部分复制主要是Redis针对全量复制的过高开销做出的一种优化措施，使用psync[runId][offset]命令实现。当从节点正在复制主节点时，如果出现网络闪断或者命令丢失等异常情况时，从节点会向主节点要求补发丢失的命令数据，主节点的复制积压缓冲区将这部分数据直接发送给从节点，这样就可以保持主从节点复制的一致性。补发的这部分数据一般远远小于全量数据。

2. 主从连接中断期间主节点依然响应命令，但因复制连接中断命令无法发送给从节点，不过主节点内的复制积压缓冲区依然可以保存最近一段时间的写命令数据。

3. 当主从连接恢复后，由于从节点之前保存了自身已复制的偏移量和主节点的运行ID。因此会把它们当做psync参数发送给主节点，要求进行部分复制。

4. 主节点接收到psync命令后首先核对参数runId是否与自身一致，如果一致，说明之前复制的是当前主节点；之后根据参数offset在复制积压缓冲区中查找，如果offset之后的数据存在，则对从节点发送+COUTINUE命令，表示可以进行部分复制。因为缓冲区大小固定，若发生缓冲溢出，则进行全量复制。

5. 主节点根据偏移量把复制积压缓冲区里的数据发送给从节点，保证主从复制进入正常状态。

哨兵
====

* 面试官：那主从复制会存在哪些问题呢？

* 我：主从复制会存在以下问题：

1. 一旦主节点宕机，从节点晋升为主节点，同时需要修改应用方的主节点地址，还需要命令所有从节点去复制新的主节点，整个过程需要人工干预。

2. 主节点的写能力受到单机的限制。

3. 主节点的存储能力受到单机的限制。

4. 原生复制的弊端在早期的版本中也会比较突出，比如：redis复制中断后，从节点会发起psync。此时如果同步不成功，则会进行全量同步，主库执行全量备份的同时，可能会造成毫秒或秒级的卡顿。

* 面试官：那比较主流的解决方案是什么呢？

* 我：当然是哨兵啊。

* 面试官：那么问题又来了。那你说下哨兵有哪些功能？

![Redis面试六]()

* 我：如图，是Redis Sentinel（哨兵）的架构图。Redis Sentinel（哨兵）主要功能包括主节点存活检测、主从运行情况检测、自动故障转移、主从切换。Redis Sentinel最小配置是一主一从。Redis的Sentinel系统可以用来管理多个Redis服务器，该系统可以执行以下四个任务：

1. 监控：不断检查主服务器和从服务器是否正常运行。

2. 通知：当被监控的某个redis服务器出现问题，Sentinel通过API脚本向管理员或者其他应用程序发出通知。

3. 自动故障转移：当主节点不能正常工作时，Sentinel会开始一次自动的故障转移操作，它会将与失效主节点是主从关系的其中一个从节点升级为新的主节点，并且将其他的从节点指向新的主节点，这样人工干预就可以免了。

4. 配置提供者：在Redis Sentinel模式下，客户端应用在初始化时连接的是Sentinel节点集合，从中获取主节点的信息。

* 面试官：那你能说下哨兵的工作原理吗？

* 我：话不多说，直接上图：

![Redis面试七]()

1. 每个Sentinel节点都需要定期执行以下任务：每个Sentinel以每秒一次的频率，向它所知的主服务器、从服务器以及其他的Sentinel实例发送一个PING命令。（如上图）

![Redis面试八]()

2. 如果一个实例距离最后一次有效回复PING命令的时间超过down-after-milliseconds所指定的值，那么这个实例会被Sentinel标记为主观下线。（如上图）

![Redis面试九]()

3. 如果一个主服务器被标记为主观下线，那么正在监视这个服务器的所有Sentinel节点，要以每秒一次的频率确认主服务器的确进入了主观下线状态。

![Redis面试十]()

4. 如果一个主服务器被标记为主观下线，并且有足够数量的Sentinel（至少要达到配置文件指定的数量）在指定的时间范围内同意这一判断，那么这个主服务器被标记为客观下线。

![Redis面试十一]()

5. 一般情况下，每个Sentinel会以每10秒一次的频率向它已知的所有主服务器和从服务器发送INFO命令，当一个主服务器被标记为客观下线时，Sentinel向下线主服务器的所有从服务器发送INFO命令的频率，会从10秒一次改为每秒一次。

![Redis面试十二]()

6. Sentinel和其他Sentinel协商客观下线的主节点的状态，如果处于SDOWN状态，则投票自动选出新的主节点，将剩余从节点指向新的主节点进行数据复制。

![Redis面试十三]()

7. 当没有足够数量的Sentinel同意主服务器下线时，主服务器的客观下线状态就会被移除。当主服务器重新向Sentinel的PING命令返回有效回复时，主服务器的主观下线状态就会被移除。

* 面试官：不错，面试前没少下工夫啊，今天Redis这关你过了，明天找个时间我们再聊聊其他的。（露出欣慰的微笑）

* 我：没问题。

总结
====

本文在一次面试的过程中讲述了Redis是什么，Redis的特点和功能，Redis缓存的使用，Redis为什么能这么快，Redis缓存的淘汰策略，持久化的两种方式，Redis高可用部分的主从复制和哨兵的基本原理。只要功夫深，铁杵磨成针，平时准备好，面试不用慌。虽然面试不一定是这样问的，但万变不离其“宗”。（笔者觉得这种问答形式的博客很不错，可读性强而且读后记的比较深刻）

参考文章
====

<a href="https://juejin.im/post/5dbef8306fb9a0203f6fa3e2#heading-3">juejin.im/post/5dbef8… juejin.im/post/5b7d22…</a>
