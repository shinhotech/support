# Cache Redis Spring Boot Starter

CacheRedisSpringBootStarter集成SpringBoot,封装@Cahceable的方法缓存的灵活处理，依赖于[Spring Data Redis](https://github.com/redisson/redisson/tree/master/redisson-spring-data#spring-data-redis-integration)模块 .

支持Spring Boot 1.3.x - 2.3.x


## 使用方法

### 1. 添加`cache-redis-spring-boot-starter`依赖到您项目:
Maven

```xml
    <dependency>
        <groupId>com.shinho</groupId>
        <artifactId>cache-redis-spring-boot-starter</artifactId>
        <version>1.0.0.RELEASE</version>
    </dependency>
```
### 2. 添加配置项 `application.yaml`
```yaml
# 1.添加redis-spring boot组件配置
spring:
  redis:
    database: 0
    host: 127.0.0.1
    password: '123456'
    port: 6379
    timeout: 60
# 2.缓存插件配置项
cache:
  redis:
    enable: true #全局是否开启缓存
    prefix: c4i_bi_ #全局生成缓存key前缀
    timeout: 1800 #全局设置缓存时间
    items: #缓存列表数组
      - enable: true   #default缓存是否开启
        name: default  #缓存名称default
        timeout: 1800  #default缓存时间
        prefix: one   #default缓存key自定义前缀
      - enable: true   #单项缓存是否开启
        name: view  #单项缓存名称
        timeout: 1800  #单项缓存时间
        prefix: two   #单项缓存key自定义前缀
```
### 3.Java代码使用

```java
    // 1. 带参数使用方法
    @PostMapping("/testCache")
    @ResponseBody
    @Cacheable(value="default",keyGenerator="keyGenerator")
    public String testCache(@RequestBody TUser user){
        System.err.println("进入testCache方法，本次传入参数:name="+user.getUsername()+",age="+user.getAge());
        return user.getUsername();
    }
    // 2. 带参数使用方法
    @PostMapping("/testCache1")
    @ResponseBody
    @Cacheable(value="view",key="#user.getAge()")
    public String testCache1(@RequestBody TUser user){
        System.err.println("进入testCache1方法，本次传入参数:name="+user.getUsername()+",age="+user.getAge());
        return user.getUsername();
    }
    // 3. 带参数使用方法
    @PostMapping("/testCache2")
    @ResponseBody
    @Cacheable(value="view")
    public String testCache2(@RequestBody TUser user){
        System.err.println("进入testCache2方法，本次传入参数:name="+user.getUsername()+",age="+user.getAge());
        return user.getUsername();
    }
    // 4. 不带参数使用方法
    @PostMapping("/testCache3")
    @ResponseBody
    @Cacheable(value="view")
    public String testCache3(@RequestBody TUser user){
        System.err.println("进入testCache3方法，本次传入参数:name="+user.getUsername()+",age="+user.getAge());
        return user.getUsername();
    }
```
### 4.注意事项：
- #### 1.@Cacheable注解使用AOP处理，相同类A的两个带有@Cacheable方法调用时，被调用方缓存会失效。

- #### 2.插件默认生成的元数据文档在META-INFO目录下，更多详细配置项可以参考元数据文档，方便快速使用。

- #### 3.遇到Aop导致@Cacheable注解失效时,A.使用@Autowired注入对象，替换this调用。B.将出问题的方法，拆分不同类调用。



