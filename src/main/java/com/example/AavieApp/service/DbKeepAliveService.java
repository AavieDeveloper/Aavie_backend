package com.example.AavieApp.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DbKeepAliveService {

    private final JdbcTemplate jdbcTemplate;

    public DbKeepAliveService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Runs every 10 minutes (600,000 ms)
    @Scheduled(fixedRate = 600000)
    public void pingDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            System.out.println("DB keep-alive ping successful");
        } catch (Exception e) {
            System.out.println("DB keep-alive ping failed: " + e.getMessage());
        }
    }
}