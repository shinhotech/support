package com.shinho.support.async.log.db.util;
import com.shinho.support.async.log.db.properties.ActionLogProperties;
import lombok.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.skywalking.apm.toolkit.trace.RunnableWrapper;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * 定时任务
 * 清理超过指定间隔的日志数据
 * @author 傅为地
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LogEvictTask {

    private JdbcTemplate jdbcTemplate;

    private ActionLogProperties actionLogProperties;

    private TaskExecutor mdcExecutor;

    /**
     * 定时清理日志表
     * 配置异步执行
     * 默认每月1号执行
     */
    @Scheduled(cron = "${action.async.log.job.cron:0 0 0 1 * ?}")
    public void clean() {
        if (actionLogProperties.isDbEnable()) {
            mdcExecutor.execute(RunnableWrapper.of(new FutureTask<Object>(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            //判断是否存在sys_action_log表
                            List<Map<String, Object>> result = jdbcTemplate.queryForList("select count(table_name) total from information_schema.tables  where table_schema = (select database()) and table_name = 'sys_action_log'");
                            if (CollectionUtils.isNotEmpty(result) && !result.get(0).get("total").toString().equals("0")) {
                                //存在表时，清理日志
                                long storage = 0 - actionLogProperties.getStorage();
                                String delSql = "delete from sys_action_log where create_time<date_add(now(), interval " + storage + " day)";
                                jdbcTemplate.execute(delSql);
                            };
                            return "ok";
                        }
                    })
            ));
        }
    }
}
