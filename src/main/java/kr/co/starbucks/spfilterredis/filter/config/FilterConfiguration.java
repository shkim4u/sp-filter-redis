package kr.co.starbucks.spfilterredis.filter.config;

import java.util.Collections;
import javax.servlet.DispatcherType;
import kr.co.starbucks.spfilterredis.filter.RequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@ConditionalOnBean(RequestFilter.class)
public class FilterConfiguration {

    private final RequestFilter requestFilter;

    @Autowired
    public FilterConfiguration(RequestFilter requestFilter) {
        this.requestFilter = requestFilter;
    }

    @Bean
    public FilterRegistrationBean<RequestFilter> requestFilterRegistration() {
        FilterRegistrationBean<RequestFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(requestFilter);
        filterRegistrationBean.setUrlPatterns(Collections.singletonList("/*"));
        filterRegistrationBean.setDispatcherTypes(DispatcherType.REQUEST);
        filterRegistrationBean.setOrder(Ordered.LOWEST_PRECEDENCE - 1);
        return filterRegistrationBean;
    }
}
