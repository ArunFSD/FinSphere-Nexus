package com.finsphere.common.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Records an {@link AuditRecord} every time the annotated method runs, with outcome and duration.
 * <p>
 * Example:
 * <pre>{@code
 * @Auditable(action = "CHIT_PLAN_CREATED", resource = "CHIT_PLAN", resourceId = "#result.id")
 * public ChitPlanResponse createPlan(ChitPlanRequest request) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /** Business action, e.g. {@code LOAN_DISBURSED}. */
    String action();

    /** Resource type, e.g. {@code LOAN}. */
    String resource();

    /**
     * Optional SpEL expression for the resource id. Method arguments are available by name ({@code #request})
     * and the return value as {@code #result} (null when the method throws).
     */
    String resourceId() default "";
}
