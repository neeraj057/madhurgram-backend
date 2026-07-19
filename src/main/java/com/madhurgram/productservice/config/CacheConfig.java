package com.madhurgram.productservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration class for setting up Spring Cache with Redis backend, 
 * utilizing clean Jackson JSON serialization and safe error handling.
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * Constructor injection for CacheConfig.
     *
     * @param redisConnectionFactory connection factory provider
     */
    public CacheConfig(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    /**
     * Creates the RedisCacheManager bean configured with JSON serialization.
     *
     * @return the cache manager bean
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        log.info("Initializing RedisCacheManager with JSON Serialization...");

        // 1. Define JSON serializer for values (non-deprecated RedisSerializer.json())
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();
        RedisSerializer<String> stringSerializer = RedisSerializer.string();

        // 2. Build default Cache Config mapping keys -> String, values -> JSON
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        // 3. Per-cache TTL overrides
        //    pincodeServiceability: 6 hours — Shiprocket serviceability results change infrequently
        RedisCacheConfiguration pincodeConfig = defaultConfig.entryTtl(Duration.ofHours(6));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(Map.of(
                        "pincodeServiceability", pincodeConfig
                ))
                .build();
    }

    /**
     * Custom cache error handler that logs Redis failures and bypasses to the database.
     * Prevents cache outages from crashing business operations.
     *
     * @return cache error handler
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis Cache GET failure for key '{}' in cache '{}'. Bypassing to Database. Error: {}", 
                        key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis Cache PUT failure for key '{}' in cache '{}'. Error: {}", 
                        key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis Cache EVICT failure for key '{}' in cache '{}'. Error: {}", 
                        key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis Cache CLEAR failure in cache '{}'. Error: {}", 
                        cache.getName(), exception.getMessage());
            }
        };
    }
}
