package kr.co.starbucks.spfilterredis.filter.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@Configuration
@ConditionalOnExpression(
    "!T(org.springframework.util.StringUtils).isEmpty('${spring.redis.principal-migration-status.host:}') and "
    + "!T(org.springframework.util.StringUtils).isEmpty('${spring.redis.principal-migration-status.port:}')"
)
public class PrincipalMigrationStatusRedisConfig {

    @Value("${spring.redis.principal-migration-status.host}")
    private String principalMigrationStatusRedistHost;

    @Value("${spring.redis.principal-migration-status.port}")
    private int principalMigrationStatusRedisPort;

    @Bean
    @Primary
    @Profile("local")
    public ReactiveRedisConnectionFactory principalMigrationStatusRedisStandaloneConnectionFactory() {
        RedisConfiguration configuration = new RedisStandaloneConfiguration(
            principalMigrationStatusRedistHost, principalMigrationStatusRedisPort);

        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
            .clientOptions(ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                    .connectTimeout(Duration.ofMillis(300)).build())
                .build())
            .commandTimeout(Duration.ofSeconds(3))
            .build();

        return new LettuceConnectionFactory(configuration, clientConfiguration);
    }

    @Bean
    @Primary
    @Profile("!local")
    public ReactiveRedisConnectionFactory principalMigrationStatusRedisClusterConnectionFactory() {
        RedisClusterConfiguration configuration = new RedisClusterConfiguration();
        configuration.clusterNode(principalMigrationStatusRedistHost,
            principalMigrationStatusRedisPort);

        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
            .clientOptions(ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                    .connectTimeout(Duration.ofMillis(300)).build())
                .build())
            .commandTimeout(Duration.ofSeconds(3))
            .build();

        return new LettuceConnectionFactory(configuration, clientConfiguration);
    }

    @Bean
    @Primary
    public ReactiveStringRedisTemplate principalMigrationStatusRedisTemplate(ReactiveRedisConnectionFactory principalMigrationStatusRedisConnectionFactory) {
        return new ReactiveStringRedisTemplate(principalMigrationStatusRedisConnectionFactory);
    }
}
