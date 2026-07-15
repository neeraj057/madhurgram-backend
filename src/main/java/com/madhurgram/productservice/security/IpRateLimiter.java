package com.madhurgram.productservice.security;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

/**
 * Thread-safe, in-memory IP rate limiter to defend public endpoints from spam bot attacks.
 */
@Component
public class IpRateLimiter {

    private final ConcurrentHashMap<String, List<Long>> requestHistory = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private static final long TIME_WINDOW_MS = TimeUnit.MINUTES.toMillis(1);

    /**
     * Checks if the given IP address is allowed to make another request.
     *
     * @param ipAddress client IP address
     * @return true if request is allowed, false if rate limit exceeded
     */
    public synchronized boolean isAllowed(String ipAddress) {
        long currentTime = System.currentTimeMillis();
        requestHistory.putIfAbsent(ipAddress, new ArrayList<>());
        
        List<Long> timestamps = requestHistory.get(ipAddress);
        
        // Remove timestamps older than the 1-minute window
        timestamps.removeIf(time -> (currentTime - time) > TIME_WINDOW_MS);
        
        if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            return false;
        }
        
        timestamps.add(currentTime);
        return true;
    }
}
