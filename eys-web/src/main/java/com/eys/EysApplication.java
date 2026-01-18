package com.eys;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.eys.mapper")
public class EysApplication {

    public static void main(String[] args) {
        SpringApplication.run(EysApplication.class, args);
    }
}
