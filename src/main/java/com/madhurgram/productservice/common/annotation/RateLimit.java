package com.madhurgram.productservice.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to enforce API rate limiting.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Maximum number of requests allowed in the window.
     */
    int limit() default 10;

    /**
     * Duration of the rate limit window in seconds.
     */
    int windowSeconds() default 60;
}
