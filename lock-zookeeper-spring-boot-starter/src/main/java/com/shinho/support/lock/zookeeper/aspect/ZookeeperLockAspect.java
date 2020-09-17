package com.shinho.support.lock.zookeeper.aspect;

import com.alibaba.fastjson.JSONObject;
import com.shinho.support.lock.zookeeper.annotation.ZkLock;
import com.shinho.support.lock.zookeeper.repository.ZookeeperLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
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
public class ZookeeperLockAspect {

    @Autowired
    private ZookeeperLock zookeeperLock;

    private ExpressionParser parser = new SpelExpressionParser();

    private LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

    @Pointcut("@annotation(com.shinho.support.lock.zookeeper.annotation.ZkLock)")
    public void initZkLockPointcut() {
    }

    /**
     * 切面处理分布式锁
     * @param point
     * @throws Throwable
     */
    @Around("initZkLockPointcut()")
    public Object doZkLockAround(ProceedingJoinPoint point) throws Throwable {
        String key="";
        int lockType=0;
        try {
            Method method = ((MethodSignature) point.getSignature()).getMethod();
            //注解别名时，需要用此种方法获取
            ZkLock zkLock = AnnotationUtils.getAnnotation(method, ZkLock.class);
            if(!ObjectUtils.isEmpty(zkLock)){
                key = parse(zkLock.value(), method, point.getArgs(), point);
                lockType=zkLock.lockType().getCode();
                if(zookeeperLock.lock(key,lockType,zkLock.holdTime(),zkLock.unit())){
                    log.debug("zookeeper locker acquire a lock success with key:{}", key);
                    return point.proceed();
                }else{
                    log.debug("zookeeper locker acquire a lock falied with key:{}", key);
                    return null;
                }
            }else{
                return point.proceed();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("zookeeper locker execute locked method occured an exception", e);
        } finally {
            zookeeperLock.releaseLock(key,lockType);
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
     * 默认生成key是类名.方法名
     */
    private String parse(String key, Method method, Object[] args, ProceedingJoinPoint point) {
        String[] params = discoverer.getParameterNames(method);
        if (!ObjectUtils.isEmpty(params)) {
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

}
