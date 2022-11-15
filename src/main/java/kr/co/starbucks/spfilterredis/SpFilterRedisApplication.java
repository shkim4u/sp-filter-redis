package kr.co.starbucks.spfilterredis;

import kr.co.starbucks.spfilterredis.redis.config.BasicEmbeddedRedisConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Disable Spring Data JPA for simple testing for filter.
 */
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
public class SpFilterRedisApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpFilterRedisApplication.class, args);
    }

}
