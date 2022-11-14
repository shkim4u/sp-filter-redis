package kr.co.starbucks.spfilterredis.filter.infrastructure.repositories;

import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class PrincipalSessionRedisReactiveRepository {

    private static final String MEMBER_SESSION_KEY = "LoginUserInfo";
    private static final String GUEST_SESSION_KEY = "LoginAppId";
    private static final String USER_TYPE_KEY = "UserType";

    private final ReactiveHashOperations<String, Object, Map<String, String>> hashMapOperations;

    private final ReactiveHashOperations<String, Object, String> hashStringOperations;

    public PrincipalSessionRedisReactiveRepository(
        @Qualifier("principalSessionRedisHashTemplate") ReactiveRedisTemplate<String, Object> sessionPrincipalTemplate
    ) {
        this.hashMapOperations = sessionPrincipalTemplate.opsForHash();
        this.hashStringOperations = sessionPrincipalTemplate.opsForHash();
    }

    public Mono<Map<String, String>> getPrincipalSession(String sessionId) {
        return hashMapOperations.get(sessionId, MEMBER_SESSION_KEY)    // 회원 조회
            .switchIfEmpty(hashMapOperations.get(sessionId, GUEST_SESSION_KEY))    // 비회원 조회
            .switchIfEmpty(Mono.empty());
    }

    public Mono<String> getUserType(String sessionId) {
        return hashStringOperations.get(sessionId, MEMBER_SESSION_KEY)   // 회원 조회
            .switchIfEmpty(hashStringOperations.get(sessionId, GUEST_SESSION_KEY))  // 비회원 조회
            .switchIfEmpty(Mono.empty());
    }
}
