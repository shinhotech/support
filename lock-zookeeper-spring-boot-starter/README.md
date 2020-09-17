# Lock Zookeeper Spring Boot Starter

LockZookeeperSpringBootStarter集成SpringBoot,@ZkLock注解的灵活开启分布式锁，依赖于[Curator](https://github.com/apache/curator)和[zookeeper](https://zookeeper.apache.org)模块 .

支持Spring Boot 1.3.x - 2.3.x,依赖的zookeeper版本(>=3.5.8)


## 使用方法

### 1. 添加`lock-zookeeper-spring-boot-starter`依赖到您项目:
Maven

```xml
    <dependency>
        <groupId>com.shinho</groupId>
        <artifactId>lock-zookeeper-spring-boot-starter</artifactId>
        <version>1.0.0.RELEASE</version>
    </dependency>
```
### 2. 添加配置项 `application.yaml`
```yaml
# 1.添加zookeeper-spring boot组件配置
spring:
  lock:
    zk:
      enable: true #开启zookeeper锁
      serverLists: 114.67.207.106:2181
```
### 3.Java代码使用

```java
     /**
        * 测试redisLock
        * 无参数
        */
       @RequestMapping(value = "/testZkLock")
       @ResponseBody
       @ZkLock(value = "#zkLockKey",lockType = ZkLockType.WRITE_LOCK,holdTime=30)
       public Boolean testZkLock() throws InterruptedException {
           System.err.println("无参数开始执行业务逻辑");
           TimeUnit.SECONDS.sleep(20);
           System.err.println("无参数业务逻辑执行完毕");
           return true;
       }
       @RequestMapping("/lock")
       public Boolean lock(){
           for(int i=0; i<10; i++){
               new RedisLockThread().start();
           }
           return true;
       }
       class RedisLockThread extends Thread {
   
           @Override
           public void run() {
               String key = "lockKey";
               boolean result = zookeeperLock.lock(key, 10000);
               log.info(result ? "get lock success : " + key : "get lock failed : " + key);
               try {
                   Thread.sleep(5000);
               } catch (InterruptedException e) {
                   log.error("exp", e);
               } finally {
                   zookeeperLock.releaseLock(key);
                   log.info("release lock : " + key);
               }
           }
       }
```
### 4.注意事项：
- #### 1.@ZkLock注解使用AOP处理，相同类A的两个带有@ZkLock方法调用时，被调用方缓存会失效。

- #### 2.插件默认支持可重入锁，互斥不可重入锁，读锁，写锁等类型，使用时请参考TUserController代码示例用法。

- #### 3.遇到Aop导致@ZkLock注解失效时,A.使用@Autowired注入对象，替换this调用。B.将出问题的方法，拆分不同类调用。



