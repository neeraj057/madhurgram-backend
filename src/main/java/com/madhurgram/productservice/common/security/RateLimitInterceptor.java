package com.madhurgram.productservice.common.security;

import com.madhurgram.productservice.common.annotation.RateLimit;
import com.madhurgram.productservice.common.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Interceptor for enforcing API request rate limits using Redis counter keys.
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String RATE_LIMIT_PREFIX = "rate:limit:";
    private static final String COLON = ":";
    private static final String COMMA_SEPARATOR = ",";
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String EXCEEDED_MSG = "Too many requests. Please try again later.";

    private final StringRedisTemplate redisTemplate;

    /**
     * Constructor injection for RateLimitInterceptor.
     *
     * @param redisTemplate string redis operations template
     */
    public RateLimitInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Inspects target API method annotations and increments rate-limiting counters in Redis.
     *
     * @param request  incoming HttpServletRequest
     * @param response outgoing HttpServletResponse
     * @param handler  execution target handler
     * @return true to continue request filter chain
     * @throws RateLimitExceededException if user exceeds rate threshold
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return true;
        }

        int limit = rateLimit.limit();
        int windowSeconds = rateLimit.windowSeconds();
        String ip = getClientIp(request);
        String methodName = handlerMethod.getMethod().getName();

        String redisKey = RATE_LIMIT_PREFIX + ip + COLON + methodName;
        
        Long count = redisTemplate.opsForValue().increment(redisKey);
        
        if (count != null && count == 1) {
            // Resolved deprecation warning: use Duration instead of long/TimeUnit
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }

        if (count != null && count > limit) {
            log.warn("Rate limit exceeded for IP: {} on method: {}. Current count: {}, limit: {}", 
                    ip, methodName, count, limit);
            throw new RateLimitExceededException(EXCEEDED_MSG);
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader(X_FORWARDED_FOR_HEADER);
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(COMMA_SEPARATOR)[0].trim();
    }
}
