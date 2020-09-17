# Lock Redisson Spring Boot Starter

LockRedissonSpringBootStarter集成SpringBoot,@LockActio注解的灵活开启分布式锁，依赖于[Spring Data Redis](https://github.com/redisson/redisson/tree/master/redisson-spring-data#spring-data-redis-integration)和[Spring Data Redisson](https://github.com/redisson/redisson/tree/master/redisson-spring-boot-starter)模块 .

支持Spring Boot 1.3.x - 2.3.x


## 使用方法

### 1. 添加`lock-redisson-spring-boot-starter`依赖到您项目:
Maven

```xml
    <dependency>
        <groupId>com.shinho</groupId>````
        <artifactId>lock-redisson-spring-boot-starter</artifactId>
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
```
### 3.Java代码使用

```java
    //启动类，加上@EnableRedissonLock
    @SpringBootApplication
    @EnableRedissonLock
    @Slf4j
    public class UserApplication {
        public static void main(String[] args) throws UnknownHostException {
            ConfigurableApplicationContext application = SpringApplication.run(UserApplication.class, args);
            Environment env = application.getEnvironment();
            String activeProfiles= StringUtils.arrayToCommaDelimitedString(env.getActiveProfiles());
            activeProfiles=StringUtils.isEmpty(activeProfiles)?"default":activeProfiles;
            log.info("<===========[{}]启动完成！"+"运行环境:[{}] IP:[{}] PORT:[{}]===========>", env.getProperty("spring.application.name"),activeProfiles, InetAddress.getLocalHost().getHostAddress(),env.getProperty("server.port"));
        }
    }    
    /**
     * 测试redisLock
     * 有参数
     */ 
    @RequestMapping(value = "/redisLock")
    @ResponseBody
    @LockAction(value = "#redisLockKey", lockType = LockType.REENTRANT_LOCK, waitTime = 30000)
    public Boolean redisLock(@RequestParam(value = "redisLockKey")  String redisLockKey) throws InterruptedException {
        System.err.println(redissonClient.getLock(redisLockKey));
        System.err.println("开始执行业务逻辑");
        testRedisLock();
        userService.testRedisLock();
        TimeUnit.SECONDS.sleep(20);
        System.err.println("业务逻辑执行完毕");
        return true;
    }
    /**
     * 测试redisLock
     * 无参数
     */
    @RequestMapping(value = "/testRedisLock")
    @ResponseBody
    @LockAction
    public Boolean testRedisLock() throws InterruptedException {
        System.err.println("无参数开始执行业务逻辑");
        TimeUnit.SECONDS.sleep(20);
        System.err.println("无参数业务逻辑执行完毕");
        return true;
    }
```
### 4.注意事项：
- #### 1.@LockAction注解使用AOP处理，相同类A的两个带有@LockAction方法调用时，被调用方缓存会失效。

- #### 2.插件默认支持可重入锁，公平锁，读锁，写锁等类型，使用时请参考TUserController代码示例用法。

- #### 3.遇到Aop导致@LockAction注解失效时,A.使用@Autowired注入对象，替换this调用。B.将出问题的方法，拆分不同类调用。



