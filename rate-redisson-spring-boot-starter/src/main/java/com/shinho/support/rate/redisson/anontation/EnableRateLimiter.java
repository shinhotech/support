package com.shinho.support.rate.redisson.anontation;

import com.shinho.support.rate.redisson.autoconfig.RateLimiterSwitchConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author 傅为地
 * redisson限流器,配置开关
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RateLimiterSwitchConfig.class)
@Documented
public @interface EnableRateLimiter {
}
