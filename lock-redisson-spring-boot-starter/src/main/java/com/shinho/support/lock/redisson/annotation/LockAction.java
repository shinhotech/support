package com.shinho.support.lock.redisson.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author 傅为地
 * redisson分布式锁注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LockAction {

	/** 锁的资源，key。支持spring El表达式，如果不赋值，默认取类名+方法名*/
	@AliasFor("key")
	String value() default "'default'";

	@AliasFor("value")
	String key() default "'default'";

	/** 获取锁等待时间，默认60秒*/
	long waitTime() default  60 * 1000L;

	/** 持锁时间 单位毫秒,默认30秒*/
	long holdTime() default 30 * 1000L;

	/** 时间单位（获取锁等待时间和持锁时间都用此单位）*/
	TimeUnit unit() default TimeUnit.MILLISECONDS;

	/** 锁类型*/
	LockType lockType() default LockType.REENTRANT_LOCK;

}
