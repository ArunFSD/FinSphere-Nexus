package com.finsphere.common.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CommonLoggingAspect {

    @Around("execution(* com.finsphere..service.*.*(..)) || execution(* com.finsphere..controller.*.*(..))")
    public Object profile(ProceedingJoinPoint joinPoint) throws Throwable {
        // This dynamically picks up the logger of the actual service class
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        logger.info(">>>> [METHOD START] {} | Params: {}", methodName, args);

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            logger.info("<<<< [METHOD END] {} | Time: {}ms", methodName, duration);
            return result;
        } catch (Exception e) {
            logger.error("!!!! [METHOD ERROR] {} | Message: {}", methodName, e.getMessage());
            throw e;
        }
    }
}