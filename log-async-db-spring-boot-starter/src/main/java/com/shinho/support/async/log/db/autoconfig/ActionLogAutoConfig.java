package com.shinho.support.async.log.db.autoconfig;
import com.shinho.support.async.log.db.aspect.ActionLogAspect;
import com.shinho.support.async.log.db.properties.ActionLogProperties;
import com.shinho.support.async.log.db.util.MdcExecutor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author 傅为地
 * 通用日志装配类
 */
@Configuration
@EnableAsync
@AutoConfigureAfter(value = {DataSourceAutoConfiguration.class})
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(ActionLogProperties.class)
@ConditionalOnProperty(prefix="action.async.log",name = "enable", havingValue = "true")
public class ActionLogAutoConfig {


    @Autowired
    private ActionLogProperties actionLogProperties;


    /**
     * 实例化日志工具
     * @return jdbcTemplate
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean(JdbcTemplate.class)
    public JdbcTemplate jdbcTemplate(@Qualifier(value="dataSource") DataSource dataSource){
        return new JdbcTemplate(dataSource);
    }

    /**
     * 实例化日志线程池
     * @return mdc线程池
     */
    @Bean(name = "mdcExecutor")
    @ConditionalOnMissingBean(MdcExecutor.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Primary
    public TaskExecutor mdcExecutor(){
        MdcExecutor executor = new MdcExecutor();
        executor.setCorePoolSize(actionLogProperties.getTask().getMin());// 线程池维护线程的最少数量
        executor.setMaxPoolSize(actionLogProperties.getTask().getMax());// 线程池维护线程的最大数量
        executor.setQueueCapacity(actionLogProperties.getTask().getQueue());//缓存队列
        executor.setThreadNamePrefix(actionLogProperties.getTask().getPrefix());// 线程池前缀
        // rejection-policy：当pool已经达到max size的时候，如何处理新任务
        // CALLER_RUNS：不在新线程中执行任务，而是由调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 对拒绝task的处理策略
        executor.setKeepAliveSeconds(actionLogProperties.getTask().getKeep());// 允许的空闲时间
        executor.initialize();
        return executor;
    }

    /**
     * 初始化日志处理
     * @param jdbcTemplate
     * @return actionLogAspect
     */
    @Bean
    @ConditionalOnBean({JdbcTemplate.class,TaskExecutor.class})
    public ActionLogAspect actionLogAspect(@Autowired JdbcTemplate jdbcTemplate){
        if(actionLogProperties.isDbEnable()){
            //判断是否存在sys_action_log表，不存在则创建
            String sql="select count(table_name) total from information_schema.tables  where table_schema = (select database()) and table_name = 'sys_action_log'";
            List<Map<String, Object>> result=jdbcTemplate.queryForList(sql);
            if(CollectionUtils.isNotEmpty(result)){
                //不存在sys_action_log表，创建
                if(result.get(0).get("total").toString().equals("0")){
                    String createSql="CREATE TABLE `sys_action_log` (\n" +
                            "  `id` bigint(64) NOT NULL AUTO_INCREMENT COMMENT '编号',\n" +
                            "  `token` varchar(500) COLLATE utf8_bin DEFAULT NULL COMMENT '用户令牌',\n" +
                            "  `trace` varchar(500) COLLATE utf8_bin DEFAULT NULL COMMENT '链路编号',\n" +
                            "  `project` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '项目名称',\n" +
                            "  `moudle` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '模块名称',\n" +
                            "  `action_type` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '操作类型',\n" +
                            "  `type` char(1) COLLATE utf8_bin DEFAULT '1' COMMENT '日志类型 1:正常 0：异常',\n" +
                            "  `request_uri` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '请求URI',\n" +
                            "  `class_name` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '执行类名',\n" +
                            "  `method_name` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '执行方法名称',\n" +
                            "  `user_agent` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '用户代理',\n" +
                            "  `remote_ip` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '操作IP地址',\n" +
                            "  `request_method` varchar(5) COLLATE utf8_bin DEFAULT NULL COMMENT '操作方式',\n" +
                            "  `request_params` text COLLATE utf8_bin COMMENT '请求参数',\n" +
                            "  `response_params` text COLLATE utf8_bin COMMENT '返回参数',\n" +
                            "  `request_mac` varchar(60) COLLATE utf8_bin DEFAULT NULL COMMENT '设备MAC',\n" +
                            "  `exception` text COLLATE utf8_bin COMMENT '异常信息',\n" +
                            "  `action_thread` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '执行线程',\n" +
                            "  `action_start_time` datetime DEFAULT NULL COMMENT '开始执行时刻',\n" +
                            "  `action_end_time` datetime DEFAULT NULL COMMENT '结束执行时刻',\n" +
                            "  `action_time` bigint(20) DEFAULT NULL COMMENT '执行耗时 单位(毫秒)',\n" +
                            "  `create_time` datetime DEFAULT NULL COMMENT '创建日志时间',\n" +
                            "  PRIMARY KEY (`id`),\n" +
                            "  KEY `sys_log_trace` (`trace`) USING BTREE\n" +
                            ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='用户日志表';";
                    jdbcTemplate.execute(createSql);
                    //存在表时，清除超过指定间隔天数的数据,默认存储时间7天
                }else{
                    long storage=0-actionLogProperties.getStorage();
                    String delSql="delete from sys_action_log where create_time<date_add(now(), interval "+storage+" day)";
                    jdbcTemplate.execute(delSql);
                }
            }
        }
        return new ActionLogAspect();
    }

}
