package com.bank.shared.exception;

import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Component;

/**
 * The single error envelope every client parses: {@code {code, message, traceId}}.
 *
 * <p>The {@code traceId} ties a client-visible error back to server traces, logs,
 * and audit records. It is resolved from the current tracing span when available.
 */
public record ApiError(String code, String message, String traceId) {

    /** Holder so the static factory can reach the (single) Tracer bean. */
    @Component
    public static class TraceIdProvider {
        private static Tracer tracer;

        public TraceIdProvider(Tracer tracer) {
            TraceIdProvider.tracer = tracer;
        }

        static String currentTraceId() {
            try {
                if (tracer != null && tracer.currentSpan() != null) {
                    return tracer.currentSpan().context().traceId();
                }
            } catch (RuntimeException ignored) {
                // tracing not active (e.g. unit test) — fall through
            }
            return "no-trace";
        }
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, TraceIdProvider.currentTraceId());
    }
}
