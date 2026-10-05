package com.tpjy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.tpjy.mapper")
public class TpjyApplication {
    public static void main(String[] args) {
        SpringApplication.run(TpjyApplication.class, args);
    }
}
