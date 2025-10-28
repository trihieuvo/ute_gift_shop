package com.utegiftshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Thêm import HttpMethod
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
					// Cho phép GET sản phẩm và danh mục công khai
					 "/api/products/**", "/api/categories", "/products/**",
					"/order-history", "/orders/**", // Cho phép xem chi tiết đơn hàng (JS sẽ gọi API kiểm tra quyền sau)
					"/css/**", "/js/**", "/images/**", "/*.ico",
					"/error",
					"/shipper/**", // Cho phép truy cập trang shipper (JS sẽ kiểm tra token và role sau)
					"/vendor/**", // Cho phép truy cập trang vendor (JS sẽ kiểm tra token và role sau)
					"/api/images/**",
					"/ws/**",
					"/v3/api-docs/**", "/swagger-ui/**",
					// Cho phép GET đánh giá sản phẩm công khai
					"/api/reviews/product/**",
                    // Cho phép GET thông tin user để kiểm tra ID khi render review
                    "/api/reviews/me"
				).permitAll()

				// ===== 2. API YÊU CẦU ĐĂNG NHẬP (BẤT KỲ VAI TRÒ) =====
				.requestMatchers(
					"/api/users/me", // GET thông tin user
                    "/api/users/me/avatar", // POST avatar
                    "/api/users/me/change-password", // POST đổi mật khẩu
                     "/api/users/me", // PUT cập nhật profile
					"/api/chat/**", // Tất cả API chat
					// GET kiểm tra điều kiện đánh giá
					 "/api/reviews/eligibility/**"
				).authenticated()

				// ===== 3. CÁC API CỦA CUSTOMER (YÊU CẦU ROLE "Customer") =====
				.requestMatchers(
					"/api/cart/**", // Tất cả API giỏ hàng
					"/api/addresses/**", // Tất cả API địa chỉ
					"/api/orders/**", // API đặt hàng, xem lịch sử, hủy đơn
					"/api/apply/**", // API để nộp đơn xin vai trò
					// POST tạo đánh giá mới và PUT cập nhật đánh giá
					"/api/reviews",
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
				.requestMatchers(
					"/api/admin/**" // API để duyệt đơn xin vai trò
				).hasAuthority("Admin")

				// ===== 7. TẤT CẢ CÁC YÊU CẦU CÒN LẠI PHẢI ĐƯỢC XÁC THỰC =====
				.anyRequest().authenticated()
			);

		http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
