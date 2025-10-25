package com.utegiftshop.config;

import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ConfigurableSiteMeshFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiteMeshConfig {

    @Bean
    public FilterRegistrationBean<ConfigurableSiteMeshFilter> siteMeshFilter() {
        FilterRegistrationBean<ConfigurableSiteMeshFilter> filter = new FilterRegistrationBean<>();
        
        filter.setFilter(new ConfigurableSiteMeshFilter() {
            @Override
            protected void applyCustomConfiguration(SiteMeshFilterBuilder builder) {
                
                // Chỉ dùng tên file - SiteMesh sẽ tự tìm trong /WEB-INF/decorators/
                
                // 1. Gán layout "auth"
                builder.addDecoratorPath("/login", "/auth_layout.html")
                       .addDecoratorPath("/signup", "/auth_layout.html")
                       .addDecoratorPath("/activate", "/auth_layout.html")
                       .addDecoratorPath("/forgot-password", "/auth_layout.html")
                       .addDecoratorPath("/reset-password", "/auth_layout.html");

                // 2. Gán layout "app"
                builder.addDecoratorPath("/home", "/app_layout.html");

                // 3. Loại trừ các API
                builder.addExcludedPath("/api/**")
                       .addExcludedPath("/v3/api-docs/**")
                       .addExcludedPath("/swagger-ui/**")
                       .addExcludedPath("/css/**")
                       .addExcludedPath("/js/**")
                       .addExcludedPath("/images/**")
                       .addExcludedPath("/error");
            }
        });
        
        filter.addUrlPatterns("/*");
        filter.setOrder(Integer.MAX_VALUE);
        return filter;
    }
}