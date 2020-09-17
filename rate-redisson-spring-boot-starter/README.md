# Rate Redisson Spring Boot Starter

RateRedissonSpringBootStarter集成SpringBoot,@RateLimiter注解的灵活开启分布式锁，依赖于[Spring Data Redis](https://github.com/redisson/redisson/tree/master/redisson-spring-data#spring-data-redis-integration)和[Spring Data Redisson](https://github.com/redisson/redisson/tree/master/redisson-spring-boot-starter)模块 .

支持Spring Boot 1.3.x - 2.3.x


## 使用方法

### 1. 添加`rate-redisson-spring-boot-starter`依赖到您项目:
Maven

```xml
    <dependency>
        <groupId>com.shinho</groupId>
        <artifactId>rate-redisson-spring-boot-starter</artifactId>
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
    //启动类，加上@EnableRateLimiter
   @SpringBootApplication
   @EnableRateLimiter
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
     * 简单测试限流器
     */
    @GetMapping(value = "/testRateLimiter")
    @RateLimiter(limit=1, timeout=1,unit = TimeUnit.MINUTES)
    @ResponseBody
    public Map<String,Object> testRateLimiter(){
        System.err.println("进入testRateLimiter方法");
        /*Map<String,Object> result=new ConcurrentHashMap<String,Object>();*/
        testRate();
        Map<String,Object> result=new HashMap<String,Object>();
        result.put("a", null);
        result.put("b", "");
        result.put("c", "马大哈");
        return result;
    }

    @GetMapping(value = "/testRate")
    @RateLimiter(limit=1, timeout=1,unit = TimeUnit.MINUTES)
    @ResponseBody
    public Map<String,Object> testRate(){
        System.err.println("进入testRate方法");
        Map<String,Object> result=new HashMap<String,Object>();
        result.put("a", null);
        result.put("b", "");
        result.put("c", "testRate");
        return result;
    }
```
### 4.注意事项：
- #### 1.@RateLimiter配置在拦截器的preHandle，开启限流一般放置在@Controller的控制器里面。

- #### 2.限流器插件基于redisson配置，默认使用总机限流(单机限流可配置)，使用时请参考TUserController代码示例用法。

- #### 3.更多的限流配置，使用方式请参考注解说明以及生成的文档说明，快速上手使用限流器的插件。



