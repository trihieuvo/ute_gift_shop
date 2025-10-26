package com.utegiftshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.utegiftshop.security.JwtAuthenticationFilter;
import com.utegiftshop.security.jwt.JwtTokenProvider;
import com.utegiftshop.security.service.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService, JwtTokenProvider jwtTokenProvider) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ===== 1. CÁC ĐƯỜNG DẪN CÔNG KHAI (KHÔNG CẦN ĐĂNG NHẬP) =====
                // BAO GỒM CẢ CÁC TRANG GIAO DIỆN CHÍNH
                .requestMatchers(
                    "/", "/home", "/login", "/signup", "/activate",
                    "/forgot-password", "/reset-password",
                    "/cart", "/checkout", "/profile", // <-- CHO PHÉP TẢI CÁC TRANG NÀY
                    "/api/auth/**",
                    "/api/users/me",
                    "/api/products/**",
                    "/css/**", "/js/**", "/images/**", "/*.ico",
                    "/error",
                    "/shipper/**",
                    "/v3/api-docs/**", "/swagger-ui/**"
                ).permitAll()

                // ===== 2. CÁC API CỦA CUSTOMER (YÊU CẦU ROLE "Customer") =====
                // ĐÂY LÀ NƠI BẢO MẬT DỮ LIỆU THỰC SỰ
                .requestMatchers(
                    
                    "/api/cart/**",
                    "/api/addresses/**",
                    "/api/orders/**"
                ).hasAuthority("Customer")

                // ===== 3. CÁC ĐƯỜNG DẪN CHO SHIPPER (YÊU CẦU ROLE "Shipper") =====
                .requestMatchers("/api/shipper/**").hasAuthority("Shipper")

                // ===== 4. TẤT CẢ CÁC YÊU CẦU CÒN LẠI PHẢI ĐƯỢC XÁC THỰC =====
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}