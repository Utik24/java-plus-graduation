package ru.practicum.comment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "ru.practicum")
@EnableDiscoveryClient
public class ExtraServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExtraServiceApplication.class, args);
    }
}