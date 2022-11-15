package kr.co.starbucks.spfilterredis.filter.infrastructure.repositories;

import kr.co.starbucks.spfilterredis.filter.config.PrincipalMigrationStatusRedisConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@ConditionalOnBean(PrincipalMigrationStatusRedisConfig.class)
public class PrincipalMigrationStatusRedisReactiveRepository {

    private final ReactiveHashOperations<String, String, String> principalMigrationStatusRedisHashStringOperations;

    public PrincipalMigrationStatusRedisReactiveRepository(ReactiveStringRedisTemplate principalMigrationStatusRedisTemplate) {
        this.principalMigrationStatusRedisHashStringOperations = principalMigrationStatusRedisTemplate.opsForHash();
    }

    public Mono<String> getPrincipalMigrationStatus(String customerId, String routeId) {
        return principalMigrationStatusRedisHashStringOperations.get(customerId, routeId);
    }
}
