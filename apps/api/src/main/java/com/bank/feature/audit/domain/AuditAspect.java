package com.bank.feature.audit.domain;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Writes an audit record after any {@code @Audited} method returns successfully.
 * High-stakes paths may also call {@link AuditService} directly with rich
 * before/after payloads; this aspect covers the common "record the action" case.
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditService audit;

    public AuditAspect(AuditService audit) {
        this.audit = audit;
    }

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void record(JoinPoint jp, Audited audited, Object result) {
        audit.write(currentActor(), audited.action(), null, result);
    }

    private String currentActor() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null ? a.getName() : "system";
    }
}
