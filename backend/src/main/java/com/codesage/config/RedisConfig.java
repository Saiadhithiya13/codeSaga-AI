package com.codesage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis configuration.
 *
 * <p>Architecture Spec §Redis Strategy: Redis is mandatory.
 * No in-memory cache fallback. All cache keys defined in {@link com.codesage.common.constants.CacheKeys}.
 *
 * <p>Uses Lettuce with connection pooling (commons-pool2) for production
 * concurrency. Connection pool settings mirror the Architecture Spec.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.lettuce.pool.max-active:10}")
    private int maxActive;

    @Value("${spring.data.redis.lettuce.pool.max-idle:5}")
    private int maxIdle;

    @Value("${spring.data.redis.lettuce.pool.min-idle:1}")
    private int minIdle;

    /**
     * Lettuce connection factory with connection pooling.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration standaloneConfig =
                new RedisStandaloneConfiguration(redisHost, redisPort);

        if (redisPassword != null && !redisPassword.isBlank()) {
            standaloneConfig.setPassword(redisPassword);
        }

        @SuppressWarnings("rawtypes")
        org.apache.commons.pool2.impl.GenericObjectPoolConfig poolConfig =
                new org.apache.commons.pool2.impl.GenericObjectPoolConfig();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .commandTimeout(Duration.ofMillis(2000))
                .build();

        return new LettuceConnectionFactory(standaloneConfig, clientConfig);
    }

    /**
     * Generic {@link RedisTemplate} for programmatic cache operations.
     * Keys are Strings; values are JSON-serialized via Jackson.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();

        return template;
    }

    /**
     * Spring Cache abstraction backed by Redis.
     * Default TTL is 10 minutes; individual caches can override this.
     */
    @Bean
    @Primary
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .build();
    }
}
