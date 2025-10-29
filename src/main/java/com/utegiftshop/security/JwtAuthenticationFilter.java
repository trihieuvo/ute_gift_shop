package com.utegiftshop.security;

import java.io.IOException;

import org.slf4j.Logger; // *** THÊM IMPORT NÀY ***
import org.slf4j.LoggerFactory; // *** THÊM IMPORT NÀY ***
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.utegiftshop.security.jwt.JwtTokenProvider;
import com.utegiftshop.security.service.UserDetailsServiceImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // *** THÊM LOGGER ***
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserDetailsServiceImpl userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Gọi hàm parseJwt đã sửa đổi
            String jwt = parseJwt(request);

            if (jwt != null && tokenProvider.validateJwtToken(jwt)) {
                String email = tokenProvider.getEmailFromJwtToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                // Log khi xác thực thành công (chỉ khi debug)
                // logger.debug("User '{}' authenticated successfully for URI: {}", email, request.getRequestURI());
            } else {
                 // Log khi token không hợp lệ hoặc không có (chỉ khi debug)
                 // if (jwt != null) logger.warn("Invalid JWT token received for URI: {}", request.getRequestURI());
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    // *** ⭐ SỬA HÀM NÀY ⭐ ***
    private String parseJwt(HttpServletRequest request) {
        // 1. Ưu tiên kiểm tra Header "Authorization"
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            // logger.debug("Found JWT in Authorization header for URI: {}", request.getRequestURI());
            return headerAuth.substring(7);
        }

        // 2. Nếu không có header, kiểm tra query parameter "token" (cho WebSocket handshake)
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            // Log để xác nhận (chỉ khi debug)
             logger.debug("Found JWT token in query parameter 'token' for URI: {}", request.getRequestURI());
            return tokenParam;
        }

        // 3. Nếu không có header, kiểm tra query parameter "access_token" (một số thư viện dùng tên này)
        String accessTokenParam = request.getParameter("access_token");
        if (StringUtils.hasText(accessTokenParam)) {
             logger.debug("Found JWT token in query parameter 'access_token' for URI: {}", request.getRequestURI());
            return accessTokenParam;
        }


        // 4. Không tìm thấy token ở cả hai nơi
        // logger.debug("No JWT token found in header or query parameters for URI: {}", request.getRequestURI());
        return null;
    }
    // *** ⭐ KẾT THÚC SỬA ⭐ ***
}