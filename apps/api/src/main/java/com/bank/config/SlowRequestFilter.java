package com.bank.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Logs requests exceeding 1 second at WARN level. */
@Component
@Order(2)
public class SlowRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SlowRequestFilter.class);
    private static final long THRESHOLD_MS = 1_000L;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= THRESHOLD_MS) {
                log.warn("[SLOW] {} {} {} {}ms",
                        request.getMethod(), request.getRequestURI(),
                        response.getStatus(), elapsed);
            }
        }
    }
}
