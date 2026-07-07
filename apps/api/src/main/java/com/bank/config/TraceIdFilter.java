package com.bank.config;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Propagates a stable trace id on every request and echoes it as {@code X-Trace-Id}.
 * When Micrometer tracing (OTel bridge) is active, the id is sourced from the
 * current span so it matches the W3C {@code traceparent} propagated to downstream
 * services (FR-29.2); otherwise it falls back to a random id.
 */
@Component
@Order(1)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    private final ObjectProvider<Tracer> tracer;

    public TraceIdFilter(ObjectProvider<Tracer> tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String traceId = currentSpanTraceId();
        if (traceId == null) {
            traceId = request.getHeader(HEADER);
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString();
            }
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String currentSpanTraceId() {
        Tracer t = tracer.getIfAvailable();
        if (t == null || t.currentSpan() == null) {
            return null;
        }
        return t.currentSpan().context().traceId();
    }
}
