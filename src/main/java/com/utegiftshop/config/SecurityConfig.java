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
				.requestMatchers(
					"/", "/home", "/login", "/signup", "/activate",
					"/forgot-password", "/reset-password",
					"/cart", "/checkout", "/profile",
					"/api/auth/**",
					"/api/products/**",
					"/order-history", "/orders/**",
					"/css/**", "/js/**", "/images/**", "/*.ico",
					"/error",
					"/shipper/**",
					"/vendor/**",
					"/api/images/**",
					"/v3/api-docs/**", "/swagger-ui/**"
				).permitAll()

				.requestMatchers(
					"/api/users/me",
					"/api/chat/**" 
				).authenticated()

				// ===== 2. CÁC API CỦA CUSTOMER (YÊU CẦU ROLE "Customer") =====
				.requestMatchers(
					"/api/cart/**",
					"/api/addresses/**",
					"/api/orders/**"
				).hasAuthority("Customer")

				.requestMatchers(
					"/api/shipper/**"
				).hasAuthority("Shipper")

				.requestMatchers(
					"/api/vendor/**"
				).hasAuthority("Vendor")

				.anyRequest().authenticated()
			);

		http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}