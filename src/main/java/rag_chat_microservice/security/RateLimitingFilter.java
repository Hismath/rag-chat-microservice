package rag_chat_microservice.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Order(Ordered.LOWEST_PRECEDENCE) // runs AFTER ApiKeyFilter
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    // Public/whitelisted paths: no rate limit
    private static final String[] PUBLIC_PATTERNS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-init.js",
            "/webjars/**",
            "/favicon.ico",
            "/actuator/**",
            "/error" // Spring Boot error endpoint
    };

    // ---- Configurable via application.properties/.env ----
    @Value("${ratelimit.permits:10}")
    private int permits;

    @Value("${ratelimit.window-seconds:60}")
    private int windowSeconds;

    @Value("${ratelimit.cache-max-size:1000}")
    private int cacheMaxSize;

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .maximumSize(1000) // will be reset in @PostConstruct
            .build();

    @jakarta.annotation.PostConstruct
    void init() {
        // reconfigure cache size from property
        cache.policy().eviction().ifPresent(ev -> ev.setMaximum(cacheMaxSize));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Always skip CORS preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getServletPath();
        for (String p : PUBLIC_PATTERNS) {
            if (MATCHER.match(p, path)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Key for rate limiting: prefer API key; (optional) fall back to client IP
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            // If your ApiKeyFilter protects all non-public paths, this should be unreachable.
            // To be safe, we still deny with 401 (aligning with ApiKeyFilter).
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized: Invalid or missing API key");
            return;
        }

        Bucket bucket = cache.get(apiKey, k -> createNewBucket());

        // Expose rate-limit headers (best effort)
        long available = bucket.getAvailableTokens();
        setRateHeaders(response, available, permits, windowSeconds);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            // RFC-friendly 429 with Retry-After (seconds)
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            response.getWriter().write("Too many requests - rate limit exceeded");
        }
    }

    private Bucket createNewBucket() {
        Refill refill = Refill.intervally(permits, Duration.ofSeconds(windowSeconds));
        Bandwidth limit = Bandwidth.classic(permits, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private void setRateHeaders(HttpServletResponse resp, long remaining, int limit, int windowSec) {
        resp.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        resp.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(remaining, 0)));
        // A simple reset hint: now + window
        long resetEpoch = Instant.now().getEpochSecond() + windowSec;
        resp.setHeader("X-RateLimit-Reset", String.valueOf(resetEpoch));
    }
}
