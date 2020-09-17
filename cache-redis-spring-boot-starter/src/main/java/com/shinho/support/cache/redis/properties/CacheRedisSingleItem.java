package com.shinho.support.cache.redis.properties;

import lombok.*;

import java.io.Serializable;

/**
 * 单项缓存配置信息
 * @author 傅为地
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@NonNull
public class CacheRedisSingleItem implements Serializable {

    /**
     *单项缓存名称
     */
    private  String name;

    /**
     * 单项缓存存活时间
     */
    private  long timeout;

    /**
     *单项缓存是否开启
     */
    private  boolean enable;

    /**
     * 单项缓存前缀
     */
    private  String prefix;

}
