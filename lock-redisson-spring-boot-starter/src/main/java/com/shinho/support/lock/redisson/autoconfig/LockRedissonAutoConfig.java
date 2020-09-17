package com.shinho.support.lock.redisson.autoconfig;

import com.shinho.support.lock.redisson.aspect.RedissonLockAspect;
import com.shinho.support.lock.redisson.properties.RedisProperties;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

/**
 * redisson分布式锁
 * 自动装配器
 * @author 傅为地
 */
@Slf4j
@Configuration
@ConditionalOnBean({LockRedissonConfig.Marker.class})
@EnableConfigurationProperties(RedisProperties.class)
public class LockRedissonAutoConfig {

	@Autowired
	private RedisProperties redisProperties;

	/**
	 * 配置redssion
	 */
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	@ConditionalOnMissingBean(RedissonClient.class)
	public RedissonClient createRedission() {
		String address="redis://"+redisProperties.getHost()+":"+redisProperties.getPort();
		Config config = new Config();
		if(!StringUtils.isEmpty(redisProperties.getPassword())){
			config.useSingleServer().setConnectTimeout(90000)
					.setTimeout(60000)
					.setPassword(redisProperties.getPassword())
					.setAddress(address);
		}else{
			config.useSingleServer().setConnectTimeout(90000)
					.setTimeout(60000)
					.setAddress(address);
		}
		return  Redisson.create(config);
	}

	@Bean
	public RedissonLockAspect createRedisLockAspect() {
		return  new RedissonLockAspect();
	}


}