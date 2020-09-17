package com.shinho.support.rate.redisson.autoconfig;

import com.shinho.support.rate.redisson.aspect.RedissonRateAspect;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * redisson限流器自动配置项
 * @author 傅为地
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnBean({RedissonClient.class,RateLimiterSwitchConfig.Marker.class})
public class RateRedissonAutoConfig {

    @Bean
    public RedissonRateAspect createRedissonRateAspect(){
        return new RedissonRateAspect();
    }

}
