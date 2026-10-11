package com.finsphere.common.logging;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Times every {@code @Service} and {@code @RestController} method of FinSphere code.
 * <p>
 * Entry, arguments and exit are logged at DEBUG only (arguments are summarised and truncated; request records
 * override {@code toString()} to hide passwords). Calls slower than
 * {@code finsphere.logging.method.slow-threshold} are logged at WARN so bottlenecks show up in the error log.
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class MethodLoggingAspect {

    private static final int MAX_ARG_LENGTH = 200;

    private final long slowThresholdNanos;

    public MethodLoggingAspect(MethodLoggingProperties properties) {
        this.slowThresholdNanos = properties.slowThreshold().toNanos();
    }

    @Pointcut("within(com.finsphere..*) && (@within(org.springframework.stereotype.Service) "
            + "|| @within(org.springframework.web.bind.annotation.RestController))")
    void applicationComponents() {
    }

    @Around("applicationComponents()")
    public Object time(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringType());
        String method = joinPoint.getSignature().toShortString();
        if (log.isDebugEnabled()) {
            log.debug(">> {} args={}", method, summarize(joinPoint.getArgs()));
        }
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            report(log, method, start, "OK");
            return result;
        }
        catch (Throwable ex) {
            report(log, method, start, ex.getClass().getSimpleName());
            throw ex;
        }
    }

    private void report(Logger log, String method, long start, String outcome) {
        long elapsed = System.nanoTime() - start;
        if (elapsed > this.slowThresholdNanos) {
            log.warn("SLOW {} took {} ms ({})", method, elapsed / 1_000_000, outcome);
        }
        else if (log.isDebugEnabled()) {
            log.debug("<< {} {} in {} ms", method, outcome, elapsed / 1_000_000);
        }
    }

    private static String summarize(Object[] args) {
        return Arrays.stream(args).map(MethodLoggingAspect::summarize).collect(Collectors.joining(", ", "[", "]"));
    }

    private static String summarize(Object arg) {
        if (arg == null) {
            return "null";
        }
        String className = arg.getClass().getName();
        if (className.startsWith("jakarta.servlet") || className.startsWith("org.apache.catalina")) {
            return arg.getClass().getSimpleName();
        }
        String text = String.valueOf(arg);
        return (text.length() > MAX_ARG_LENGTH) ? text.substring(0, MAX_ARG_LENGTH) + "..." : text;
    }
}
