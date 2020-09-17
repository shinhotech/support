package com.shinho.support.lock.redisson.annotation;

import com.shinho.support.lock.redisson.autoconfig.LockRedissonConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author 傅为地
 * redisson分布式锁,配置开关
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(LockRedissonConfig.class)
@Documented
public @interface EnableRedissonLock {
}
