package com.shinho.support.lock.zookeeper.autoconfig;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.shinho.support.lock.zookeeper.aspect.ZookeeperLockAspect;
import com.shinho.support.lock.zookeeper.properties.ZookeeperLockProperties;
import com.shinho.support.lock.zookeeper.repository.ZookeeperLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**  
 * @author 傅为地
 * zookeeper分布式锁自动装配
 * 配置开关
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(ZookeeperLockProperties.class)
@ConditionalOnProperty(prefix="spring.lock.zk",name = "enable", havingValue = "true")
public class ZookeeperLockAutoConfiguration {
	
	@Autowired
	private ZookeeperLockProperties zookeeperLockProperties;

	/**
	 * 配置Curator工厂
	 */
	@Bean
	public CuratorFramework curatorFramework(){
		ZookeeperLockProperties.Zookeeper zkConfig = zookeeperLockProperties.getZk();
		log.info("zookeeper lock auto config registry center init, server lists is: {}.", zkConfig.getServerLists());
		CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
				.connectString(zkConfig.getServerLists())
				.retryPolicy(new ExponentialBackoffRetry(zkConfig.getBaseSleepTimeMilliseconds(),
						zkConfig.getMaxRetries(), zkConfig.getMaxSleepTimeMilliseconds()))
				.namespace(zkConfig.getNamespace());
		if (0 != zkConfig.getSessionTimeoutMilliseconds()) {
			builder.sessionTimeoutMs(zkConfig.getSessionTimeoutMilliseconds());
		}
		if (0 != zkConfig.getConnectionTimeoutMilliseconds()) {
			builder.connectionTimeoutMs(zkConfig.getConnectionTimeoutMilliseconds());
		}
		if (!Strings.isNullOrEmpty(zkConfig.getDigest())) {
			builder.authorization("digest", zkConfig.getDigest().getBytes(Charsets.UTF_8))
					.aclProvider(new ACLProvider() {
						@Override
						public List<ACL> getDefaultAcl() {
							return ZooDefs.Ids.CREATOR_ALL_ACL;
						}
						@Override
						public List<ACL> getAclForPath(final String path) {
							return ZooDefs.Ids.CREATOR_ALL_ACL;
						}
					});
		}
		CuratorFramework curatorFramework = builder.build();
		curatorFramework.start();
		try {
			if (!curatorFramework.blockUntilConnected(zkConfig.getMaxSleepTimeMilliseconds() * zkConfig.getMaxRetries(), TimeUnit.MILLISECONDS)) {
				curatorFramework.close();
				throw new KeeperException.OperationTimeoutException();
			}
		} catch (Exception e) {
			log.error("zookeeper lock auto config exception", e);
		}
        return curatorFramework;
	}

	/**
	 * 实例化zookeeper分布式锁对象
	 * @param curatorFramework
	 * @return ZookeeperLock
	 */
	@Bean
	@ConditionalOnBean(CuratorFramework.class)
	public ZookeeperLock createZookeeperLock(@Qualifier("curatorFramework") CuratorFramework curatorFramework, ZookeeperLockProperties zookeeperLockProperties){
		return new ZookeeperLock(curatorFramework,zookeeperLockProperties);
	}

	/**
	 * 实例化zookeeper分布式锁切面
	 * @return ZookeeperLockAspect
	 */
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	@ConditionalOnBean(ZookeeperLock.class)
	public ZookeeperLockAspect createZookeeperLockAspect(){
		return new ZookeeperLockAspect();
	}
}
