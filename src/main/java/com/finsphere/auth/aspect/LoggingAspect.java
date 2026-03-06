package com.finsphere.auth.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Dynamic Pointcut: This targets every method in every class
     * within the 'com.finsphere' package and its sub-packages.
     */
    @Pointcut("execution(* com.finsphere..*(..))")
    public void applicationPackagePointcut() {
    }

    @Before("applicationPackagePointcut()")
    public void logBefore(JoinPoint joinPoint) {
        // 1. Get Method Metadata
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        // 2. Identify the User (Security Context)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = (auth != null && auth.isAuthenticated()) ? auth.getName() : "Anonymous";

        // 3. Log the Hit
        // We use {} placeholders for high-performance logging with SLF4J
        log.info(">>>> [AUDIT] User: {} | Hitting: {}.{} | Params: {}",
                user, className, methodName, Arrays.toString(args));
    }
}
