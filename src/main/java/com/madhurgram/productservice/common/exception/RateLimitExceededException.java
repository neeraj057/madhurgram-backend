package com.madhurgram.productservice.common.exception;

/**
 * Custom exception thrown when a client exceeds their API rate limit.
 */
public class RateLimitExceededException extends RuntimeException {
    
    public RateLimitExceededException(String message) {
        super(message);
    }
}
