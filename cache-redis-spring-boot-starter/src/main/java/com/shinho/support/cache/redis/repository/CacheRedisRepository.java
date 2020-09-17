package com.shinho.support.cache.redis.repository;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.*;
import java.util.concurrent.Callable;

/**
 * @author 傅为地
 * 自定义配置redis缓存
 * 自定义生成key策略，目前分开了不同的缓存name区：
 * 如当前cacheName的名字是defalut：那么就增删改查时，都会生成的key带上前缀:projectName+"_fn_"+cacheName+"_"],类似于ehcache功能
 */
@Slf4j
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("unchecked")
public class CacheRedisRepository implements Cache {

    //缓存固定名称
    private String name;

    //缓存存活时间,默认24小时
    private long timeout=60*60*24;

    //某项缓存是否开启,默认开启
    private boolean enable=true;

    //单项最终缓存前缀
    private String keyPrefix;

    private RedisTemplate redisTemplate;

    private RedisConnectionFactory connectionFactory;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return new RedisCacheManager(this.redisTemplate).getCache(name);
    }

    /**
     * 获取缓存数据
     *
     * @param key
     * @return null
     */
    @Override
    public ValueWrapper get(Object key) {
        if (enable) {
            final String keyf = getUkPrfex(key.toString());
            Object object = null;
            object = redisTemplate.execute(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    byte[] key = keyf.getBytes();
                    byte[] value = connection.get(key);
                    if (value == null) {
                        return null;
                    }
                    return toObject(value);
                }
            });
            return (object != null ? new SimpleValueWrapper(object) : null);
        } else {
            return null;
        }
    }

    /**
     * 方法结果存入缓存
     *
     * @param key
     * @param value
     */
    @Override
    public void put(Object key, Object value) {
        if (enable) {
            final String keyf = getUkPrfex(key.toString());
            final Object valuef = value;
            final long liveTime = timeout;
            redisTemplate.execute(new RedisCallback<Long>() {
                @Override
                public Long doInRedis(RedisConnection connection)
                        throws DataAccessException {
                    byte[] keyb = keyf.getBytes();
                    byte[] valueb = toByteArray(valuef);
                    connection.set(keyb, valueb);
                    if (liveTime > 0) {
                        connection.expire(keyb, liveTime);
                    }
                    return 1L;
                }
            });
        }
    }

    /**
     * 清除
     */
    @Override
    public void evict(Object key) {
        if (enable) {
            redisTemplate.delete(getUkPrfex(key.toString()));
        }
    }

    /**
     * 清除的时候，只会清除缓存名称为name前缀的缓存
     */
    @Override
    public void clear() {
        if (enable) {
            redisTemplate.delete(redisTemplate.keys(getUkPrfex("*")));
        }
    }

    /**
     * 从缓存获取参数
     *
     * @param key
     * @param type
     * @param <T>
     * @return <T>
     */
    @Override
    public <T> T get(Object key, Class<T> type) {
        if (enable) {
            Object object = null;
            try {
                ValueOperations<String, Object> valueops = redisTemplate.opsForValue();
                object = valueops.get(getUkPrfex(key.toString()));
            } catch (IllegalStateException e) {
                e.printStackTrace();
                log.error("redis cache get object error key:{},type:{},error:{}", key, type, e);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("redis cache get object error key:{},type:{},error:{}", key, type, e);
            }
            return isEmpty(object) ? null : (T) object;
        } else {
            return null;
        }
    }

    /**
     * 从缓存获取参数
     *
     * @param key
     * @param valueLoader
     * @param <T>
     * @return <T>
     */
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        if (enable) {
            Object object = null;
            try {
                object = get(key, valueLoader.call().getClass());
            } catch (Exception e) {
                e.printStackTrace();
                log.error("redis cache get object error key:{},valueLoader:{},error:{}", key, valueLoader, e);
            }
            return (T) object;
        } else {
            return null;
        }

    }

    /**
     * 自动将指定值在缓存中指定的键是否已经设置
     */
    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        if (enable) {
            ValueWrapper vw = get(key);
            if (isEmpty(vw)) {
                put(key, value);
                return null;
            } else {
                return vw;
            }
        } else {
            return null;
        }
    }

    /*保证生成的key唯一前缀*/
    private String getUkPrfex(String key) {
        return key.startsWith(keyPrefix+"_fn_" + name + "_") ? key : (keyPrefix+"_fn_" + name + "_" + key);
    }

    /*判断对象是否为空*/
    private boolean isEmpty(Object obj) {
        return obj == null || obj.toString().trim().equals("") || obj.toString().trim().length() == 0;
    }

    /**
     * 对象转换字节流
     *
     * @param obj
     * @return bytes
     */
    private byte[] toByteArray(Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray();
            oos.close();
            bos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            log.error("redis cache convent object to byteArray error object:{},error：", obj, ex);
        }
        return bytes;
    }

    /**
     * 字节流转换对象
     *
     * @param bytes
     * @return obj
     */
    private Object toObject(byte[] bytes) {
        Object obj = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bis);
            obj = ois.readObject();
            ois.close();
            bis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            log.error("redis cache convent byteArray to object error bytes:{},error：", bytes, ex);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            log.error("redis cache convent byteArray to object error bytes:{},error：", bytes, ex);
        }
        return obj;
    }

}