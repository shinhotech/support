package com.shinho.support.lock.redisson.aspect;

import com.alibaba.fastjson.JSONObject;
import com.shinho.support.lock.redisson.annotation.LockAction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.RedissonMultiLock;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * redisson分布式锁切面处理
 *
 * @author 付为地
 */
@Aspect
@Slf4j
public class RedissonLockAspect {

    @Autowired
    private RedissonClient redissonClient;

    private final String REDISSON_LOCK_PRIEX="redisson_lock_";// 默认生成的redisson分布式锁前缀

    private ExpressionParser parser = new SpelExpressionParser();

    private LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

    @Pointcut("@annotation(com.shinho.support.lock.redisson.annotation.LockAction)")
    public void initRedissonLockPointcut() {
    }

    /**
     * 切面处理分布式锁
     *
     * @param point
     * @throws Throwable
     */
    @Around("initRedissonLockPointcut()")
    public Object doRedissonLockAround(ProceedingJoinPoint point) throws Throwable {
        RLock lock = null;
        String key = null;
        try {
            Method method = ((MethodSignature) point.getSignature()).getMethod();
            //注解别名时，需要用此种方法获取
            LockAction lockAction = AnnotationUtils.getAnnotation(method, LockAction.class);
            if(!ObjectUtils.isEmpty(lockAction)){
                key = REDISSON_LOCK_PRIEX+parse(lockAction.value(), method, point.getArgs(), point);
                lock = getLock(key, lockAction);
                if (lock.tryLock(lockAction.waitTime(), lockAction.holdTime(), lockAction.unit())) {
                    log.debug("redisson distributed get lock success with key:{}", key);
                    return point.proceed();
                }else{
                    log.debug("redisson distributed get lock failed with key:{}", key);
                    return null;
                }
            }else{
                return point.proceed();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("redisson distributed get lock exception with key:{}", key);
        } finally {
            if (!ObjectUtils.isEmpty(lock)) {
                log.debug("redisson distributed release lock success with key:{}", key);
                lock.unlock();
            } else {
                log.debug("redisson distributed system auto release lock success with key:{}", key);
            }
        }
        return null;
    }

    /**
     * @param key    表达式
     * @param method 方法
     * @param args   方法参数
     * @param point  切面对象
     * @return
     * @description 解析spring EL表达式,无参数方法
     */
    private String parse(String key, Method method, Object[] args, ProceedingJoinPoint point)  {
        String[] params = discoverer.getParameterNames(method);
        //指定spel表达式，并且有适配参数时
        if (!ObjectUtils.isEmpty(params)&&!StringUtils.isEmpty(key)) {
            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < params.length; i++) {
                context.setVariable(params[i], args[i]);
            }
            return parser.parseExpression(key).getValue(context, String.class);
        } else {
            return parseDefaultKey(key, method, point.getArgs(), point);
        }
    }

    /**
     *  生成key的分三部分，类名+方法名，参数,key
     *  不满足指定SPEL表达式并且有适配参数时，
     *  采用本方法生成最终redis的限流器key
     * @param key
     * @param method
     * @param args
     * @param point
     * @return
     */
    private String parseDefaultKey(String key, Method method, Object[] args, ProceedingJoinPoint point){
        Map<String, Object> keyMap = new LinkedHashMap<String, Object>();//保证key有序
        keyMap.put("target", point.getTarget().getClass().toGenericString());//放入target的名字
        keyMap.put("method", method.getName());//放入method的名字
        //把所有参数放进去
        Object[] params=point.getArgs();
        if (!ObjectUtils.isEmpty(params)) {
            for (int i = 0; i <params.length ; i++) {
                keyMap.put("params-" + i, params[i]);
            }
        }
        byte[] argsHash = null;
        String cacheKey = null;
        try {
            //key表达式
            key= StringUtils.isEmpty(key)?"":"_"+key;
            argsHash = MessageDigest.getInstance("MD5").digest((JSONObject.toJSON(keyMap).toString()+key).getBytes("UTF-8"));
            cacheKey= DigestUtils.md5Hex(argsHash).toUpperCase();//使用MD5生成位移key
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            log.error("RedissonRateAspect create cache key error:",e);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.error("RedissonRateAspect create cache key error:",e);
        }
        return cacheKey;
    }
    /**
     * 获取指定类型锁
     *
     * @param key
     * @param lockAction
     * @return
     */
    private RLock getLock(String key, LockAction lockAction) {
        switch (lockAction.lockType()) {
            case REENTRANT_LOCK:
                return redissonClient.getLock(key);
            case FAIR_LOCK:
                return redissonClient.getFairLock(key);
            case READ_LOCK:
                return redissonClient.getReadWriteLock(key).readLock();
            case WRITE_LOCK:
                return redissonClient.getReadWriteLock(key).writeLock();
            case RED_LOCK:
                return new RedissonRedLock(redissonClient.getLock(key));
            case MULTI_LOCK:
                return new RedissonMultiLock(redissonClient.getLock(key));
            default:
                log.error("do not support lock type:" + lockAction.lockType().name());
                throw new RuntimeException("do not support lock type:" + lockAction.lockType().name());
        }
    }
}
