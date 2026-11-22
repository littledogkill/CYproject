package org.chengyuan.cyproject.config;

import org.chengyuan.cyproject.filter.JwtFilter;
import org.chengyuan.cyproject.util.JwtUtil;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtFilterConfig {

    private final JwtUtil jwtUtil;

    public JwtFilterConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new JwtFilter(jwtUtil));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}
