package com.utegiftshop.ute_giftshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories; 

@SpringBootApplication(
    scanBasePackages = "com.utegiftshop"
)
@EnableJpaRepositories("com.utegiftshop.repository") 
@EntityScan("com.utegiftshop.entity")
public class UteGiftshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(UteGiftshopApplication.class, args);
    }

}