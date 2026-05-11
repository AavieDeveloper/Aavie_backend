package com.example.AavieApp.config;

import com.example.AavieApp.filter.RequestLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestLoggingConfig {
    
    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> loggingFilterRegistration(
            RequestLoggingFilter requestLoggingFilter) {
        FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(requestLoggingFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(2);
        return registration;
    }
}