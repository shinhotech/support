package com.shinho.support.rate.redisson.anontation;

import org.redisson.api.RateType;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author 傅为地
 * redisson限流器，注解配置项
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {

    /** redisson限流器key,支持spring El表达式，如果不赋值，默认取类名+方法名*/
    @AliasFor(attribute="key")
    String value() default "";

    @AliasFor(attribute="value")
    String key() default "";

    /** 放行数量，默认50个 */
    long limit() default 50L;//放行数量,50个

    /** 限流时间间隔数量,默认1 */
    long timeout() default 1L;//限流时间间隔，默认1秒

    /** 重试次数，默认0不重试 */
    long retryTime() default 0L;

    /** 时间单位（获取锁等待时间和持锁时间都用此单位）*/
    TimeUnit unit() default TimeUnit.SECONDS;

    /** 限流类型，默认总机限流*/
    RateType type()  default RateType.OVERALL;
}