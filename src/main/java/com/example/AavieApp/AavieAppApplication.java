package com.example.AavieApp;

import java.util.TimeZone;


import org.springframework.scheduling.annotation.EnableScheduling;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AavieAppApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(AavieAppApplication.class, args);
    }
}
