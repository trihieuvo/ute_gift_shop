package com.utegiftshop.ute_giftshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean; // Thêm import      
import org.springframework.data.jpa.repository.config.EnableJpaRepositories; // Thêm import
import org.springframework.web.client.RestTemplate; // Thêm import

@SpringBootApplication(
    scanBasePackages = "com.utegiftshop"
)
@EnableJpaRepositories("com.utegiftshop.repository") 
@EntityScan("com.utegiftshop.entity")
public class UteGiftshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(UteGiftshopApplication.class, args);
    }
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
  
}