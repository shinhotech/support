package com.shinho.support.cache.redis.autoconfig;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.shinho.support.cache.redis.properties.CacheRedisProperties;
import com.shinho.support.cache.redis.properties.CacheRedisSingleItem;
import com.shinho.support.cache.redis.repository.CacheRedisRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @author 傅为地
 * redis方法级缓存使用配置项
 * 全局开启时，才会加载该模块
 */
@Slf4j
@Configuration
@EnableCaching
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableConfigurationProperties(CacheRedisProperties.class)
@ConditionalOnProperty(prefix="cache.redis",name = "enable", havingValue = "true")
public class CacheRedisAutoConfig extends CachingConfigurerSupport {

    @Autowired
    private CacheRedisProperties cacheRedisProperties;

    /**
     * 自定义SpringCache缓存key
     */
    @Bean("keyGenerator")
    @Primary
    @ConditionalOnMissingBean(KeyGenerator.class)
    @Override
    public KeyGenerator keyGenerator() {
        return new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                Map<String, Object> keyMap = new LinkedHashMap<String, Object>();//保证key有序
                keyMap.put("target", target.getClass().toGenericString());//放入target的名字
                keyMap.put("method", method.getName());//放入method的名字
                //把所有参数放进去
                if (!ObjectUtils.isEmpty(params)) {
                    for (int i = 0; i <params.length ; i++) {
                        keyMap.put("params-" + i, params[i]);
                    }
                }
                byte[] argsHash = null;
                String cacheKey = null;
                try {
                    argsHash = MessageDigest.getInstance("MD5").digest(new Gson().toJson(keyMap).getBytes("UTF-8"));
                    cacheKey= DigestUtils.md5Hex(argsHash).toUpperCase();//使用MD5生成位移key
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    log.error("AutoCacheRedisConfig init springCache keyGenerator error:",e);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    log.error("AutoCacheRedisConfig init springCache keyGenerator error:",e);
                }
                return cacheKey;
            }
        };
    }

    /**
     * 二级缓存实例
     * @param redisConnectionFactory
     * @return redisTemplate
     */
    @Bean(name="mybatisCache")
    @Order(value =Ordered.HIGHEST_PRECEDENCE)
    @Primary
    @ConditionalOnClass({RedisConnectionFactory.class})
    public RedisTemplate<String, Object> mybatisCache(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        // 设置值（value）的序列化采用Jackson2JsonRedisSerializer。
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        // 设置键（key）的序列化采用StringRedisSerializer。
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // 设置值（hashKey）的序列化采用Jackson2JsonRedisSerializer。
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        // 设置键（hashValue）的序列化采用Jackson2JsonRedisSerializer。
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();
        redisTemplate.setEnableTransactionSupport(true);//开启事务支持
        return redisTemplate;
    }

    /**
     * 自定义缓存SimpleCacheManager
     */
    @Bean
    @Order(value =Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnBean({RedisTemplate.class,RedisConnectionFactory.class})
    public SimpleCacheManager simpleCacheManager(RedisTemplate<Object, Object> redisTemplate,RedisConnectionFactory factory) {
        SimpleCacheManager simple = new SimpleCacheManager();
        if(!CollectionUtils.isEmpty(cacheRedisProperties.getItems())){
            String keyPrefix=StringUtils.isEmpty(cacheRedisProperties.getPrefix())?"":cacheRedisProperties.getPrefix();
            long globalTimeOut=ObjectUtils.isEmpty(cacheRedisProperties.getTimeout())?60*60*24:cacheRedisProperties.getTimeout();
            //祛除重复，过滤掉null
            List<CacheRedisSingleItem> items=new ArrayList<CacheRedisSingleItem>(new HashSet<CacheRedisSingleItem>(cacheRedisProperties.getItems()));
            Set<CacheRedisRepository> caches=new HashSet<CacheRedisRepository>();
            Iterator<CacheRedisSingleItem> it=items.iterator();
            while (it.hasNext()){
                CacheRedisSingleItem item=it.next();
                if(!ObjectUtils.isEmpty(item)){
                    //缓存前缀
                    String allkeyPrefix=!StringUtils.isEmpty(item.getPrefix())?keyPrefix+item.getPrefix():keyPrefix+"";
                    //是否开启
                    boolean isEnable=ObjectUtils.isEmpty(item.isEnable())?Boolean.TRUE:item.isEnable();
                    //配置缓存
                    if(!StringUtils.isEmpty(item.getName())){
                        //配置默认超时时间
                        if(ObjectUtils.isEmpty(item.getTimeout())){
                            caches.add(new CacheRedisRepository(item.getName(),globalTimeOut,isEnable,allkeyPrefix,redisTemplate,factory));
                        }else{
                            caches.add(new CacheRedisRepository(item.getName(),item.getTimeout(),isEnable,allkeyPrefix,redisTemplate,factory));
                        }
                    }
                }
            }
            simple.setCaches(caches);
        }
        return simple;
    }

}
