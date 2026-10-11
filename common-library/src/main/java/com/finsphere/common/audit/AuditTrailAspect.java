package com.finsphere.common.audit;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.common.exception.DomainException;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Implements {@link Auditable}: times the method and records success or failure in the {@link AuditTrail}.
 * <p>
 * Runs outside the transaction advice (higher precedence), so a transaction that fails at commit time is
 * recorded as a failure rather than a success.
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class AuditTrailAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailAspect.class);

    private final AuditTrail auditTrail;

    private final ExpressionParser parser = new SpelExpressionParser();

    private final ParameterNameDiscoverer parameterNames = new DefaultParameterNameDiscoverer();

    /** Parsed SpEL expressions are immutable and thread-safe, parse each one once. */
    private final Map<String, Expression> expressions = new ConcurrentHashMap<>();

    public AuditTrailAspect(AuditTrail auditTrail) {
        this.auditTrail = auditTrail;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            this.auditTrail.event(auditable.action(), auditable.resource())
                .resourceId(resolveResourceId(auditable, joinPoint, result))
                .durationMs(elapsedMillis(start))
                .record();
            return result;
        }
        catch (Throwable ex) {
            this.auditTrail.event(auditable.action(), auditable.resource())
                .resourceId(resolveResourceId(auditable, joinPoint, null))
                .failure(describe(ex))
                .durationMs(elapsedMillis(start))
                .record();
            throw ex;
        }
    }

    private @Nullable Object resolveResourceId(Auditable auditable, ProceedingJoinPoint joinPoint,
            @Nullable Object result) {
        if (auditable.resourceId().isBlank()) {
            return null;
        }
        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(null, method,
                    joinPoint.getArgs(), this.parameterNames);
            context.setVariable("result", result);
            Expression expression = this.expressions.computeIfAbsent(auditable.resourceId(),
                    this.parser::parseExpression);
            return expression.getValue(context);
        }
        catch (RuntimeException ex) {
            log.debug("Could not resolve audit resource id '{}': {}", auditable.resourceId(), ex.getMessage());
            return null;
        }
    }

    private static String describe(Throwable ex) {
        if (ex instanceof DomainException domain) {
            return domain.getStatus().value() + " " + domain.getMessage();
        }
        return ex.getClass().getSimpleName() + ((ex.getMessage() != null) ? ": " + ex.getMessage() : "");
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
