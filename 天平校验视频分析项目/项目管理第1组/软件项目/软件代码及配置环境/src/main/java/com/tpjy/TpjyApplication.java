package com.tpjy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.tpjy.mapper")
@EnableScheduling
public class TpjyApplication {
    public static void main(String[] args) {
        SpringApplication.run(TpjyApplication.class, args);
    }
}
