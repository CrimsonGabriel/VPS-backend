package com.bazunia.vps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableAsync
public class VpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(VpsApplication.class, args);
    }

}