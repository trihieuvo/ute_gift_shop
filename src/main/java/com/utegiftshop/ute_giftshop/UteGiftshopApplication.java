package com.utegiftshop.ute_giftshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories; // <-- IMPORT DÒNG NÀY

@SpringBootApplication(
    scanBasePackages = "com.utegiftshop",
    exclude = {SecurityAutoConfiguration.class}
)
@EnableJpaRepositories("com.utegiftshop.repository") 
@EntityScan("com.utegiftshop.entity")
public class UteGiftshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(UteGiftshopApplication.class, args);
    }

}