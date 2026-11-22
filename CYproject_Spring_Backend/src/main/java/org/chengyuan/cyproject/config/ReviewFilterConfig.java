package org.chengyuan.cyproject.config;

import org.chengyuan.cyproject.filter.ReviewFilter;
import org.chengyuan.cyproject.util.AIReview;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class ReviewFilterConfig {

    private final AIReview aiReview;

    private final ObjectMapper objectMapper;

    private final StringRedisTemplate postRedisTemplate;

    public ReviewFilterConfig(AIReview aiReview, ObjectMapper objectMapper,  StringRedisTemplate postRedisTemplate) {
        this.aiReview = aiReview;
        this.objectMapper = objectMapper;
        this.postRedisTemplate = postRedisTemplate;
    }

    @Bean
    public FilterRegistrationBean<ReviewFilter> filterRegistrationBean() {
        FilterRegistrationBean<ReviewFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ReviewFilter(objectMapper, aiReview, postRedisTemplate));
        registration.addUrlPatterns("/*");
        registration.setOrder(2);
        return registration;
    }
}
