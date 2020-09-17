package com.shinho.support.cache.redis.properties;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author 傅为地
 * 自定义缓存配置项
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix="cache.redis")
public class CacheRedisProperties {

	/**
	 *全局缓存开关
	 */
	private boolean enable=true;

	/**
	 *全局缓存时长,默认24小时
	 */
	private long timeout=60*60*24;

	/**
	 *全局默认生成的key前缀
	 */
	private String prefix="cache_";

	/**
	 *全部缓存集合，每项缓存不为空或null,且不重复
	 */
    private List<CacheRedisSingleItem> items;

}
