package kr.co.starbucks.spfilterredis.redis.config;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

@Configuration
public class BasicEmbeddedRedisConfig {

    @Value("6379")
    private int redisPort;

    private RedisServer redisServer;

    @PostConstruct
    public void setRedisServer() {
        redisServer = new RedisServer(redisPort);
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}
