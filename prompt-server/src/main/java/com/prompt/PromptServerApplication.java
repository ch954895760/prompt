package com.prompt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.prompt.mapper")
public class PromptServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromptServerApplication.class, args);
    }

}
