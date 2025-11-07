package com.bazunia.vps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan; // <-- DODAJ TEN IMPORT

// ⭐️ DODANA KLUCZOWA LINIA ⭐️
@ComponentScan(basePackages = "com.bazunia.vps") 
@SpringBootApplication
public class VpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(VpsApplication.class, args);
    }

}