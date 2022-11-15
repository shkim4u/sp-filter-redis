package kr.co.starbucks.spfilterredis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import kr.co.starbucks.spfilterredis.filter.enums.RouteId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
//@Profile("!local")
public class SampleService {

    private final ReactiveRedisConnectionFactory principalSessionRedisConnectionFactory;

    private final ReactiveHashOperations<String, Object, Map<String, String>> principalSessionRedisHashMapOperations;
    private final ReactiveHashOperations<String, Object, String> principalSessionRedisHashStringOperations;

    private final ReactiveRedisConnectionFactory principalMigrationStatusRedisConnectionFactory;
    private final ReactiveHashOperations<String, String, String> principalMigrationStatusRedisHashStringOperations;

    private static final AtomicInteger count = new AtomicInteger(0);

    public SampleService(
        ReactiveRedisConnectionFactory principalSessionRedisConnectionFactory,
        @Qualifier("principalSessionRedisHashTemplate") ReactiveRedisTemplate<String, Object> principalSessionRedisHashTemplate,
        ReactiveRedisConnectionFactory principalMigrationStatusRedisConnectionFactory,
        ReactiveStringRedisTemplate principalMigrationStatusRedisTemplate
    ) {
        // Session redis.
        this.principalSessionRedisConnectionFactory = principalSessionRedisConnectionFactory;
        this.principalSessionRedisHashMapOperations = principalSessionRedisHashTemplate.opsForHash();
        this.principalSessionRedisHashStringOperations = principalSessionRedisHashTemplate.opsForHash();

        // Migration status redis.
        this.principalMigrationStatusRedisConnectionFactory = principalMigrationStatusRedisConnectionFactory;
        this.principalMigrationStatusRedisHashStringOperations = principalMigrationStatusRedisTemplate.opsForHash();
    }

    public void addTestUser() {
        log.info("[SampleService] Adding a test user.");

        // Step 1: Session redis.
        String sessionId = "3CB361E0BE1A9A7DE7DB926DF0772BAE";
        Map<String, String> userInfo = Map.of("userId", "test", "userName", "test", "sckMbbrNo", "20220712112147hilbmq");
        principalSessionRedisConnectionFactory.getReactiveConnection()
            .serverCommands()
            .flushAll()
            .then(principalSessionRedisHashMapOperations.put(sessionId, "LoginUserInfo", userInfo))
            .subscribe();


        // Step 2: Migration status redis values.
        List<Entry<String, String>> principalMigrationStatusEntries = new ArrayList<>();
        // Default testing entries borrowed from "sck-gateway" project (reverse proxy).
        principalMigrationStatusEntries.add(Map.entry("APP_SP_ROUTE", "ACTIVE"));
        principalMigrationStatusEntries.add(Map.entry("WEB_SP_ROUTE", "ACTIVE"));
        // Brand new entry to check migration status for this service application.
        principalMigrationStatusEntries.add(Map.entry(RouteId.XO_SP_ROUTE.name(), "ACTIVE"));

        Flux<Entry<String, String>> principalMigrationStatusEntriesFlux = Flux.fromIterable(principalMigrationStatusEntries);
        principalMigrationStatusRedisConnectionFactory.getReactiveConnection()
            .serverCommands()
            .save()
//            .flushAll()
            .thenMany(principalMigrationStatusEntriesFlux.flatMap(entry -> principalMigrationStatusRedisHashStringOperations.put("20220712112147hilbmq", entry.getKey(), entry.getValue())))
            .subscribe();

        // Print the added active entries.
        principalMigrationStatusRedisHashStringOperations.entries("20220712112147hilbmq")
            .doOnNext(entry -> {
                log.info("[{}] {}:{}", "20220712112147hilbmq", entry.getKey(), entry.getValue());
            }).subscribe();
    }
}
