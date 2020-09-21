package com.shinho.support.lock.redisson.autoconfig;

import com.shinho.support.lock.redisson.aspect.RedissonLockAspect;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redisson分布式锁
 * 自动装配器
 * @author 傅为地
 */
@Slf4j
@Configuration
@ConditionalOnBean({LockRedissonConfig.Marker.class,RedissonClient.class})
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class LockRedissonAutoConfig {

	@Bean
	public RedissonLockAspect createRedisLockAspect() {
		return  new RedissonLockAspect();
	}


}