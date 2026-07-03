package com.bank.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-principal token-bucket rate limiter (defense in depth behind the edge
 * gateway). Keyed by the authenticated subject when present, else by client IP.
 * Returns 429 with the standard error envelope shape when the bucket is empty.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();   // 100 req / min / principal
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {
        Bucket bucket = buckets.computeIfAbsent(principalKey(req), k -> newBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write(
                    "{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests\",\"traceId\":\"no-trace\"}");
        }
    }

    private String principalKey(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return "tok:" + Integer.toHexString(auth.hashCode());
        }
        return "ip:" + req.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // never rate-limit health/metrics scraping
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }
}
