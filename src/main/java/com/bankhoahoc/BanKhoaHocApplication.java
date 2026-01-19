package com.bankhoahoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.bankhoahoc.repository")
@EntityScan(basePackages = "com.bankhoahoc.entity")
public class BanKhoaHocApplication {

    public static void main(String[] args) {
        SpringApplication.run(BanKhoaHocApplication.class, args);
    }
}
