package com.yb.icgapi.icpic.infrastructure.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
@Slf4j
public class ControllerLogAspect {
    
    @Pointcut("execution(* com.yb.icgapi.controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Before("controllerPointcut()")
    public void beforeMethod(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.info("调用接口: {}.{}", className, methodName);
        
        // 获取所有参数
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest) {
                HttpServletRequest request = (HttpServletRequest) arg;
                log.info("请求方法: {}", request.getMethod());
                log.info("Query参数: {}", request.getQueryString());
            } else if (!isBasicType(arg)) {
                // 打印非基本类型的参数(通常是请求体)
                log.info("请求参数: {}", arg);
            }
        }
    }

    private boolean isBasicType(Object arg) {
        return arg instanceof String || arg instanceof Number || arg instanceof Boolean || 
               arg instanceof Character || arg == null;
    }
}