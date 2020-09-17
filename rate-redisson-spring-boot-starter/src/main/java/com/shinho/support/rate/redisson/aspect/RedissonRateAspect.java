package com.shinho.support.rate.redisson.aspect;

import com.alibaba.fastjson.JSONObject;
import com.shinho.support.rate.redisson.anontation.RateLimiter;
import com.shinho.support.rate.redisson.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpStatus;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * redisson分布式限流器切面处理
 * @author 付为地
 */
@Aspect
@Slf4j
public class RedissonRateAspect {

    @Autowired
    private RedissonClient redissonClient;

    private ExpressionParser parser = new SpelExpressionParser();

    private LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

    private final String REDISSON_RATE_LIMITER_PRIEX="redisson_rate_limiter_";// 默认生成的redisson限流前缀

    @Pointcut("@annotation(com.shinho.support.rate.redisson.anontation.RateLimiter)")
    public void initRedissonRateAspectPointcut() {
    }

    /**
     * 切面处理redisson限流器
     * @param point
     * @throws Throwable
     */
    @Around("initRedissonRateAspectPointcut()")
    public Object doRedissonRateAround(ProceedingJoinPoint point) throws Throwable {
        String key = null;
        try {
            Method method = ((MethodSignature) point.getSignature()).getMethod();
            //注解别名时，需要用此种方法获取
            RateLimiter rateLimiter = AnnotationUtils.getAnnotation(method, RateLimiter.class);
            if (!ObjectUtils.isEmpty(rateLimiter)) {
                key = REDISSON_RATE_LIMITER_PRIEX+parse(rateLimiter.value(), method, point.getArgs(), point);
                if(tryAcquire(redissonClient, key, rateLimiter.limit(), rateLimiter.timeout(), rateLimiter.unit().name(),rateLimiter.type(),rateLimiter.retryTime())){
                    log.debug("redisson rate limiter obtained the token success with key:{}", key);
                    return point.proceed();
                }else{
                    log.error("redisson rate limiter block the request with key:{}",key);
                    throw new RateLimitException("redisson rate limiter block the request ", HttpStatus.SERVICE_UNAVAILABLE.value());
                }
            }else{
                return point.proceed();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("redisson rate limiter obtained the token error with key:{},error:{}",key,e);
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
     * 没有参数默认使用完整类名+方法名+key
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
            key=StringUtils.isEmpty(key)?"":"_"+key;
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
     * 直接使用redisson限流器
     * 参考:https://github.com/redisson/redisson/wiki
     *   限流器redisson3.7.1版本
     *   本处默认配置都是秒，可以手动修改
     *   重启服务，可以手动清除一下redis库
     * @param redisClient
     * @param rateKey：自定义key
     * @param permits:允许最大限流数
     * @param timeout:时间间隔单位数
     * @param timeUnit:时间间隔类型，默认秒(SECONDS,MINUTES,HOURS,DAYS)
     * @param type:限流类型，默认秒(OVERALL 总机限流，PER_CLIENT 单机限流)
     * @param retryTimes:重试次数，默认0不重试
     */
    private  boolean tryAcquire(RedissonClient redisClient, String rateKey, long permits, long timeout, String timeUnit, RateType type, long retryTimes) {
        RRateLimiter rateLimiter = redisClient.getRateLimiter(rateKey);
        //初始化,最大流速
        /*rateLimiter.trySetRate(RateType.OVERALL, 1000, 1, RateIntervalUnit.SECONDS);//1秒1000个token*/
        switch (timeUnit.toUpperCase()) {
            case "SECONDS":
                rateLimiter.trySetRate(type, permits, timeout, RateIntervalUnit.SECONDS);//1秒permits个token
                return rateLimiter.tryAcquire(1,retryTimes, TimeUnit.SECONDS);//间隔一秒，尝试一次，失败就不重试，直接拒绝访问，0换成正数，就会间隔重试
            case "MINUTES":
                rateLimiter.trySetRate(type, permits, timeout, RateIntervalUnit.MINUTES);//1分钟permits个token
                return rateLimiter.tryAcquire(1,retryTimes, TimeUnit.MINUTES);//间隔一分钟，尝试一次，失败就不重试，直接拒绝访问，0换成正数，就会间隔重试
            case "HOURS":
                rateLimiter.trySetRate(type, permits, timeout, RateIntervalUnit.HOURS);//1小时permits个token
                return rateLimiter.tryAcquire(1,retryTimes, TimeUnit.HOURS);//间隔一小时，尝试一次，失败就不重试，直接拒绝访问，0换成正数，就会间隔重试
            case "DAYS":
                rateLimiter.trySetRate(type, permits, timeout, RateIntervalUnit.DAYS);//1天permits个token
                return rateLimiter.tryAcquire(1,retryTimes, TimeUnit.DAYS);//间隔一天，尝试一次，失败就不重试，直接拒绝访问，0换成正数，就会间隔重试
            default:
                rateLimiter.trySetRate(type, permits, timeout, RateIntervalUnit.SECONDS);//1秒permits个token
                return rateLimiter.tryAcquire(1,retryTimes, TimeUnit.SECONDS);//间隔一秒，尝试一次，失败就不重试，直接拒绝访问，0换成正数，就会间隔重试
        }
    }
}
