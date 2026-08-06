package com.ecommerce.api_gateway.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIP(request);
        String requestURI = request.getRequestURI();

        boolean isAuthEndpoint = requestURI.startsWith("/api/auth/");
        ConsumptionProbe probe = isAuthEndpoint
                ? rateLimiterService.tryConsumeAuth(clientIp)
                : rateLimiterService.tryConsumeGeneral(clientIp);

        long capacity = isAuthEndpoint
                ? rateLimiterService.getAuthCapacity()
                : rateLimiterService.getGeneralCapacity();

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitForRefillSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("Retry-After", String.valueOf(waitForRefillSeconds));

            log.warn("Rate limit exceeded for IP: {} on URI: {}. Retry after {} seconds.",
                    clientIp, requestURI, waitForRefillSeconds);

            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "statusCode", HttpStatus.TOO_MANY_REQUESTS.value(),
                    "message", "Too many requests. Please try again in " + waitForRefillSeconds + " seconds.",
                    "timestamp", Instant.now().toString()
            ));
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
