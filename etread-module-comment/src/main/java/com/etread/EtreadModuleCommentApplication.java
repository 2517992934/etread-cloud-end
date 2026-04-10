package com.etread;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.etread")
@MapperScan("com.etread.mapper")
@EnableScheduling
@EnableDiscoveryClient
public class EtreadModuleCommentApplication {

    public static void main(String[] args) {
        SpringApplication.run(EtreadModuleCommentApplication.class, args);
        System.out.println("Etread comment module started.");
    }
}
