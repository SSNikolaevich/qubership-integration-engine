package org.qubership.integration.platform.engine.configuration;

import java.util.function.Function;

import org.apache.camel.spi.IdempotentRepository;
import org.qubership.integration.platform.engine.camel.idempotency.IdempotentRepositoryParameters;
import org.qubership.integration.platform.engine.camel.idempotency.RedisIdempotentRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfiguration
public class RedisAutoConfiguration {
    private static final String IDEMPOTENT_REPOSITORY_KEY_PROPERTY = "IdempotentRepositoryKey";

    @Bean
    RedisTemplate<String, String> redisTemplate(
        RedisConnectionFactory redisConnectionFactory
    ) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(name = "idempotentRepositoryFactory")
    Function<IdempotentRepositoryParameters, IdempotentRepository> idempotentRepository(
        RedisTemplate<String, String> redisTemplate,
        ObjectMapper objectMapper
    ) {
        return keyParameters -> new RedisIdempotentRepository(
            redisTemplate,
            objectMapper,
            keyParameters,
            (key, exchange) -> exchange.setProperty(IDEMPOTENT_REPOSITORY_KEY_PROPERTY, key)
        );
    }
}
