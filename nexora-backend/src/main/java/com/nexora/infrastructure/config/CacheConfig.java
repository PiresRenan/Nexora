package com.nexora.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

/**
 * Configuração do Redis como backend de cache.
 * Define TTLs diferentes por cache name — produtos expiram mais rápido que categorias.
 * Condicionado à presença de Redis configurado.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class CacheConfig {

    public static final String CACHE_PRODUCTS   = "products";
    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_STOCK      = "stock";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        var jsonSerializer = new GenericJackson2JsonRedisSerializer();
        var serializationPair = RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer);

        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(serializationPair)
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues();

        var cacheConfigs = Map.of(
                CACHE_PRODUCTS,   defaultConfig.entryTtl(Duration.ofMinutes(5)),
                CACHE_CATEGORIES, defaultConfig.entryTtl(Duration.ofMinutes(30)),
                CACHE_STOCK,      defaultConfig.entryTtl(Duration.ofMinutes(1))
        );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}