package com.shinho.support.lock.redisson.autoconfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redisson分布式锁，
 * 模块启动开关
 * @author 傅为地
 */
@Configuration
public class LockRedissonConfig {
    @Bean
    public Marker enableLockRedissonMarker(){
        return new Marker();
    }
    class Marker{}
}
