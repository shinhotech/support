package com.shinho.support.async.log.db.annotation;

import java.lang.annotation.*;

/**
 * 用户操作日志
 * 日志打印注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ActionLog {
    /**
     * 操作模块
     */
    String moudle() default "";
    /**
     * 操作类型
     */
    String actionType() default "";

    /**
     * 是否存储数据库
     */
    boolean isSaveDb() default true;
}