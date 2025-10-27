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
            // --- CÁC QUY TẮC PHÂN QUYỀN ---
            .authorizeHttpRequests(auth -> auth

                // ===== 1. CÁC ĐƯỜNG DẪN CÔNG KHAI (permitAll) =====
                // Định nghĩa các quy tắc này ĐẦU TIÊN và giữ chúng cụ thể
                .requestMatchers(
                    // Tài nguyên tĩnh
                    "/css/**", "/js/**", "/images/**", "/*.ico", "/error",
                    // Các trang công khai
                    "/", "/home", "/login", "/signup", "/activate",
                    "/forgot-password", "/reset-password",
                    // Các API công khai
                    "/api/auth/**",       // API Đăng nhập/Đăng ký
                    "/api/products/**",   // API xem sản phẩm công khai
                    "/api/categories",    // API xem danh mục công khai
					"/admin/**",
                    // Swagger
                    "/v3/api-docs/**", "/swagger-ui/**"

                ).permitAll()

                // ===== 2. QUY TẮC CỦA ADMIN (hasAuthority "Admin") =====
                // Phải đặt TRƯỚC các quy tắc authenticated chung
                .requestMatchers("/api/v1/admin/**").hasAuthority("Admin") // API của Admin (Đường dẫn đúng)

                // ===== 3. QUY TẮC CỦA VENDOR (hasAuthority "Vendor") =====
                .requestMatchers("/vendor/**").hasAuthority("Vendor")       // Trang HTML của Vendor
                .requestMatchers("/api/v1/vendor/**").hasAuthority("Vendor") // API của Vendor

                // ===== 4. QUY TẮC CỦA SHIPPER (hasAuthority "Shipper") =====
                .requestMatchers("/shipper/**").hasAuthority("Shipper")     // Trang HTML của Shipper
                .requestMatchers("/api/shipper/**").hasAuthority("Shipper") // API của Shipper (nếu có)

                // ===== 5. QUY TẮC CỦA CUSTOMER (hasAuthority "Customer") =====
                .requestMatchers(
                    "/profile",             // Trang Profile
                    "/cart",                // Trang Giỏ hàng
                    "/checkout",            // Trang Thanh toán
                    "/order-history",       // Trang Lịch sử đơn hàng
                    "/orders/**",           // Trang Chi tiết đơn hàng
                    "/api/cart/**",         // API Giỏ hàng
                    "/api/addresses/**",    // API Địa chỉ
                    "/api/orders/**",       // API Đơn hàng
                    "/api/shops/register",  // API Đăng ký shop
                    "/api/shops/my"         // API Kiểm tra trạng thái shop
                    // Thêm các đường dẫn khác chỉ dành cho customer ở đây
                ).hasAuthority("Customer")

                // ===== 6. CÁC API CẦN ĐĂNG NHẬP (Bất kỳ ai) =====
                // Đặt SAU các quy tắc role cụ thể nhưng TRƯỚC anyRequest
                .requestMatchers(
                    "/api/users/me",      // API lấy thông tin user hiện tại
                    "/api/chat/**"        // API Chat (nếu có)
                ).authenticated()

                // ===== 7. TẤT CẢ CÁC YÊU CẦU CÒN LẠI =====
                // Quy tắc này PHẢI đặt CUỐI CÙNG
                .anyRequest().authenticated() // Mặc định: Yêu cầu đăng nhập cho mọi thứ không được liệt kê ở trên
            );
            // --- KẾT THÚC CÁC QUY TẮC PHÂN QUYỀN ---

        // Thêm bộ lọc JWT trước bộ lọc username/password mặc định
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}