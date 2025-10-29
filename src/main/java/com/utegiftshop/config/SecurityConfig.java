package com.utegiftshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager; // Thêm import HttpMethod
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
				.requestMatchers(
					"/", "/home", "/login", "/signup", "/activate",
					"/forgot-password", "/reset-password",
					"/cart", "/checkout", "/profile",
					"/api/auth/**",
					 "/api/products/**", "/api/categories", "/products/**",
					"/order-history", "/orders/**", 
					"/css/**", "/js/**", "/images/**", "/*.ico",
					"/error",
					"/shipper/**", 
					"/vendor/**", 
                    "/admin/**",
					"/api/images/**",
					"/ws/**",
					"/v3/api-docs/**", "/swagger-ui/**",
					"/api/reviews/product/**",
                    "/api/reviews/me",
					"/payment/**",
					"/api/payment/**"
					
				).permitAll()

				// ===== 2. API YÊU CẦU ĐĂNG NHẬP (BẤT KỲ VAI TRÒ) =====
				.requestMatchers(
					"/api/users/me", 
                    "/api/users/me/avatar", 
                    "/api/users/me/change-password", 
                    "/api/users/me", 
					"/api/chat/**", 
					"/api/addresses/**", // Tất cả API địa chỉ
					"/api/cart/**",
					"/api/reviews/eligibility/**",
					"/api/orders/**"
					
					
				).authenticated()

				// ===== 3. CÁC API CỦA CUSTOMER (YÊU CẦU ROLE "Customer") =====
				.requestMatchers(
					"/api/cart/**", // Tất cả API giỏ hàng
					
					"/api/orders/**", // API đặt hàng, xem lịch sử, hủy đơn
					"/api/apply/**", // API để nộp đơn xin vai trò
					"/api/reviews/*" // Cho phép PUT /api/reviews/{reviewId}
				).hasAuthority("Customer")

				// ===== 4. CÁC ĐƯỜNG DẪN CHO SHIPPER (YÊU CẦU ROLE "Shipper") =====
				.requestMatchers(
					"/api/shipper/**"
				).hasAuthority("Shipper")

				// ===== 5. CÁC ĐƯỜNG DẪN CHO VENDOR (YÊU CẦU ROLE "Vendor") =====
				.requestMatchers(
					"/api/vendor/**"
				).hasAuthority("Vendor")

				// ===== 6. CÁC ĐƯỜNG DẪN CHO ADMIN (YÊU CẦU ROLE "Admin") =====
				.requestMatchers("/api/v1/admin/**").hasAuthority("Admin")

				// ===== 7. TẤT CẢ CÁC YÊU CẦU CÒN LẠI PHẢI ĐƯỢC XÁC THỰC =====
				.anyRequest().authenticated()
			);

		http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
