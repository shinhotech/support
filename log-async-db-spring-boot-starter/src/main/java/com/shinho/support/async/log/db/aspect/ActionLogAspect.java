package com.shinho.support.async.log.db.aspect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shinho.support.async.log.db.annotation.ActionLog;
import com.shinho.support.async.log.db.properties.ActionLogProperties;
import com.shinho.support.async.log.db.util.MacInfoUtil;
import com.shinho.support.async.log.db.util.RequestUtil;
import com.shinho.support.async.log.db.util.RtException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.skywalking.apm.toolkit.trace.RunnableWrapper;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * 日志打印切面处理
 * @author 傅为地
 */
@Aspect
@Slf4j
public class ActionLogAspect {

    private String trace="";//微服务请求链路编号

    private String token="";//微服务用户操作token

    private String responseParams="";//返回结果

    private String requestUrl="";//请求地址

    private String userAgent="";//用户代理

    private String remoteIp="";//请求IP

    private String requestMethod="";//请求方式

    private String className="";//请求类名

    private String methodName="";//请求方法名

    private String threadName="";//线程名称

    private String requestParams="";//请求参数

    private String requestMac="";//请求的mac

    private Date beginTime=null;//开始时间

    private Date endTime=null;//结束时间

    @Autowired
    ActionLogProperties actionLogProperties;

    @Autowired
    TaskExecutor mdcExecutor;

    @Autowired
    JdbcTemplate jdbcTemplate;


    /* 切入日志打印 */
    @Pointcut("@annotation(com.shinho.support.async.log.db.annotation.ActionLog)")
    public void actionLogAspectPrint() {
    }

    /* 日志打印 方法执行(前/后)，开启skuwalking日志追踪 */
    @Trace
    @Around("actionLogAspectPrint()")
    public Object doAroundActionLogAspectPrint(ProceedingJoinPoint pjp) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        Method method = ((org.aspectj.lang.reflect.MethodSignature) pjp.getSignature()).getMethod();
        ActionLog actionLog = AnnotationUtils.getAnnotation(method, ActionLog.class);
        /**
         * 微服务全局请求链路编号，
         * 1.可以采用自定义网关生成的traceId。
         * 2.也可以使用skywaliking自带的全局链路traceId
         * 3.本处优先使用本处采用skywalking自带的traceId，记录请求链路编号
         */
        trace= StringUtils.isNotEmpty(TraceContext.traceId())?TraceContext.traceId():request.getHeader(actionLogProperties.getTrace());
        trace=StringUtils.isNotEmpty(trace)?trace:"";
        /**
         * 用户请求微服务的token
         */
        token=StringUtils.isNotEmpty(request.getHeader(actionLogProperties.getToken()))?request.getHeader(actionLogProperties.getToken()):"";
        requestUrl=request.getRequestURI();//请求地址
        userAgent=request.getHeader("user-agent");//用户代理
        remoteIp=RequestUtil.getRemoteIp(request);//请求的IP
        requestMethod=request.getMethod();//请求方法
        //将trace和token放入MDC中，供子线程获取数据
        MDC.put(actionLogProperties.getTrace(), trace);
        MDC.put(actionLogProperties.getToken(), token);
        className = pjp.getTarget().getClass().getName();//类名
        methodName = method.getName();//方法名
        requestMac=MacInfoUtil.getMac();//电脑mac
        Object[] params = pjp.getArgs();//参数列表
        if (requestMethod.equals("GET")) {
            requestParams=request.getQueryString();
        }else{
            //阿里的JSON针对文件格式，存在问题，这里使用谷歌的工具
            requestParams=!ObjectUtils.isEmpty(params)?new GsonBuilder().serializeNulls().create().toJson(params):"";
        }
        threadName=Thread.currentThread().getName();//当前线程名称
        //开启注解
        if (!ObjectUtils.isEmpty(actionLog) && actionLogProperties.isEnable()) {
            //操作模块
            String moudle = actionLog.moudle();
            //操作类型
            String actionType = actionLog.actionType();
            Object result = null;
            //开始时间
            beginTime=new Date();
            try {
                //GET参数时，URLDecoder解析
                requestParams=requestMethod.equals("GET")&&StringUtils.isNotEmpty(requestParams)? URLDecoder.decode(requestParams,"UTF-8"):requestParams;
                log.info("开始 令牌[{}],链路[{}],项目[{}],模块[{}],类型[{}]，类名[{}],方法名[{}],AGENT[{}],URL[{}],方式[{}],MAC[{}],IP[{}],参数[{}]",
                        token,trace,actionLogProperties.getProject(),moudle, actionType, className, methodName, userAgent, requestUrl, requestMethod, requestMac, remoteIp,
                        requestParams);
                result = pjp.proceed();// result的值就是被拦截方法的返回值
                //结束时间
                endTime=new Date();
                //日志记录文件
                if (ObjectUtils.isEmpty(result)) {
                    log.info("完成 令牌[{}],链路[{}],项目[{}],模块[{}],类型[{}]，类名[{}],方法名[{}],AGENT[{}],URL[{}],方式[{}],MAC[{}],IP[{}],参数[{}],返回[{}]",
                            token,trace,actionLogProperties.getProject(),moudle, actionType, className, methodName, userAgent, requestUrl, requestMethod, requestMac, remoteIp,
                            requestParams, result);
                } else {
                    responseParams=result instanceof String?result.toString():new GsonBuilder().serializeNulls().create().toJson(result);
                    log.info("完成 令牌[{}],链路[{}],项目[{}],模块[{}],类型[{}]，类名[{}],方法名[{}],AGENT[{}],URL[{}],方式[{}],MAC[{}],IP[{}],参数[{}],返回[{}]",
                            token,trace,actionLogProperties.getProject(),moudle, actionType, className, methodName, userAgent, requestUrl, requestMethod, requestMac, remoteIp,
                            requestParams, responseParams);
                }
                //开启日志记录数据库,启用线程池
                if (actionLogProperties.isDbEnable()&&actionLog.isSaveDb()) {
                    /*日志写入数据库,子线程抛出异常，方便主线程捕获*/
                    FutureTask<Object> task = new FutureTask<Object>(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            String sql="INSERT INTO `sys_action_log`(`token`,`trace`,`project`,`moudle`,`action_type`,`type`,`request_uri`,`class_name`,`method_name`,`user_agent`,`remote_ip`,`request_method`,`request_params`,`response_params`,`request_mac`,`exception`,`action_thread`,`action_start_time`,`action_end_time`,`action_time`,`create_time`)\n" +
                                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                            return jdbcTemplate.update(sql,token,trace,actionLogProperties.getProject(),moudle,actionType,"1",requestUrl,className,methodName,userAgent,remoteIp,requestMethod,requestParams,responseParams
                                    , requestMac,null,threadName,beginTime,endTime,endTime.getTime()-beginTime.getTime(),new Date())>0;
                        }
                    });
                    /**
                     * 配置skywalking跨线程追踪
                     * https://skyapm.github.io/document-cn-translation-of-skywalking/zh/8.0.0/setup/service-agent/java-agent/Application-toolkit-trace-cross-thread.html
                     */
                    mdcExecutor.execute(RunnableWrapper.of(task));    //为提升访问速率, 日志记录采用异步的方式进行.
                    //不开启日志记录数据库，只记录文件
                }
                return result;
            } catch (Throwable e) {
                endTime=new Date();//结束时间
                log.error("ActionLogAspect with exception occurred：" + e);
                /*日志写入数据库,子线程抛出异常，也可以在子线程内部try-catch然后再把异常抛出，主线程处理
                 * 开启数据库异常日志时，记录数据库日志，抛出异常让全局异常处理,
                 */
                if (actionLogProperties.isDbEnable()&&actionLog.isSaveDb()) {
                    FutureTask<Object> task = new FutureTask<Object>(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            //存储异常堆栈信息到数据库,获取具体堆栈异常日志
                            StringBuffer errStack=new StringBuffer(2048);
                            errStack.append(e.toString());
                            StackTraceElement[] stackArr=e.getStackTrace();
                            if(!ObjectUtils.isEmpty(stackArr)){
                                for (StackTraceElement stack: stackArr) {
                                    errStack.append("\n\tat " + stack);
                                }
                            }
                            String sql="INSERT INTO `sys_action_log`(`token`,`trace`,`project`,`moudle`,`action_type`,`type`,`request_uri`,`class_name`,`method_name`,`user_agent`,`remote_ip`,`request_method`,`request_params`,`response_params`,`request_mac`,`exception`,`action_thread`,`action_start_time`,`action_end_time`,`action_time`,`create_time`)\n" +
                                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                            return jdbcTemplate.update(sql,token,trace,actionLogProperties.getProject(),moudle,actionType,"0",requestUrl,className,methodName,userAgent,remoteIp,requestMethod,requestParams,responseParams
                                    , requestMac,errStack.toString(),threadName,beginTime,endTime,endTime.getTime()-beginTime.getTime(),new Date())>0;
                        }
                    });
                    mdcExecutor.execute(RunnableWrapper.of(task));    //为提升访问速率, 日志记录采用异步的方式进行.
                }
                throw new RtException(e.toString(), HttpStatus.INTERNAL_SERVER_ERROR.value(),e);
            }
        } else {
            try {
                return pjp.proceed();
            } catch (Throwable e) {
                e.printStackTrace();
                log.error("ActionLogAspect with exception occurred：" + e);
                throw new RtException(e.toString(), HttpStatus.INTERNAL_SERVER_ERROR.value(),e);
            }
        }
    }


}
