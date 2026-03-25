package com.demo.bpm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BpmApplication {

    public static void main(String[] args) {
        SpringApplication.run(BpmApplication.class, args);
    }
}
