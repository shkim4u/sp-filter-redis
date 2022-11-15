package kr.co.starbucks.spfilterredis.filter.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnExpression(
    "!T(org.springframework.util.StringUtils).isEmpty('${spring.redis.principal-session.host:}') and "
        + "!T(org.springframework.util.StringUtils).isEmpty('${spring.redis.principal-session.port:}')"
)
public class PrincipalSessionRedisConfig {

    @Value("${spring.redis.principal-session.host}")
    private String principalSessionRedisHost;

    @Value("${spring.redis.principal-session.port}")
    private int principalSessionRedisPort;

    @Bean(name = "principalSessionRedisConnectionFactory")
    public ReactiveRedisConnectionFactory principalSessionRedisConnectionFactory() {
        RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration(
            principalSessionRedisHost, principalSessionRedisPort);
        standaloneConfiguration.setPassword(RedisPassword.none());
        return new LettuceConnectionFactory(standaloneConfiguration);
    }

    @Bean(name = "principalSessionRedisHashTemplate")
    public ReactiveRedisTemplate<String, Object> principalSessionRedisHashTemplate(
        @Qualifier("principalSessionRedisConnectionFactory") ReactiveRedisConnectionFactory factory
    ) {
        RedisSerializationContext<String, Object> context = RedisSerializationContext
            .<String, Object>newSerializationContext()
            .key(new StringRedisSerializer())
            .value(new JdkSerializationRedisSerializer())
            .hashKey(new JdkSerializationRedisSerializer())
            .hashValue(new JdkSerializationRedisSerializer())
            .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }

}
