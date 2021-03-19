package com.shinho.support.async.log.db.properties;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 傅为地
 * 用户操作自定义日志配置项
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "action.async.log")
public class ActionLogProperties {

    /**
     * 是否开启日志
     */
    private boolean enable=true;

    /**
     * 是否开启日志记录数据库
     */
    private boolean dbEnable=true;

    /**
     * 项目名称，多项目配置
     */
    private String project="";

    /**
     * 微服务网关链路key
     */
    private String trace="GLOBAL_LOG_PRIFIX";

    /**
     * 微服务用户令牌key
     */
    private String token="X_AMZ_SECURITY_TOKEN";

    /**
     * 微服务用户名key
     */
    private String user="adminLoginUserName";

    /**
     * 存储天数,默认存储最近30天
     */
    private long storage=30;

    /**
     * 日志清理任务
     */
    private Job job=new Job();

    /**
     * 日志清理任务
     */
    @Data
    public class Job{

        /**
         * 是否开启任务
         */
        private boolean enable=true;

        /**
         * 清理日志任务表达式,每月1执行一次
         */
        private String cron="0 0 0 1 * ?";
    }

    /**
     * 日志线程池
     */
    private Task task=new Task();
    /**
     * 日志线程池配置
     */
    @Data
    public class Task{

        /**
         * 日志线程池，核心线程数
         */
        private int core=20;

        /**
         * 日志线程池，最大线程数
         */
        private int max=40;

        /**
         * 日志线程池，缓冲队列数
         */
        private int queue=200;

        /**
         * 日志线程池，线程名称前缀
         */
        private String prefix="async-mdc-task-executor-";

        /**
         * 日志线程池，允许的空闲时间
         */
        private int keep=60;

    }


}
