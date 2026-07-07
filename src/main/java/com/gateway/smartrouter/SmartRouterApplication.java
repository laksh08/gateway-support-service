package com.gateway.smartrouter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartRouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartRouterApplication.class, args);
    }
}
