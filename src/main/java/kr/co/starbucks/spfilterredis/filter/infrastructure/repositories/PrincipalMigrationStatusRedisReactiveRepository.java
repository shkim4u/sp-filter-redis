package kr.co.starbucks.spfilterredis.filter.infrastructure.repositories;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class PrincipalMigrationStatusRedisReactiveRepository {

    private final ReactiveHashOperations<String, String, String> principalMigrationStatusRedisHashStringOperations;

    public PrincipalMigrationStatusRedisReactiveRepository(ReactiveStringRedisTemplate principalMigrationStatusRedisTemplate) {
        this.principalMigrationStatusRedisHashStringOperations = principalMigrationStatusRedisTemplate.opsForHash();
    }

    public Mono<String> getPrincipalMigrationStatus(String customerId, String routeId) {
//        return principalMigrationStatusRedisHashStringOperations.get(customerId, routeId).switchIfEmpty(Mono.just(""));
        Mono<String> ret = principalMigrationStatusRedisHashStringOperations.get(customerId, routeId).switchIfEmpty(Mono.empty());

        return ret;
    }
}
