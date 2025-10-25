package com.utegiftshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.utegiftshop.dto.request.ResetPasswordRequest;
import com.utegiftshop.security.service.EmailService;
import java.sql.Timestamp;
import java.util.Random;
import java.util.Optional;
import org.springframework.web.bind.annotation.RequestParam;
import com.utegiftshop.dto.request.LoginRequest;
import com.utegiftshop.dto.request.SignupRequest;
import com.utegiftshop.dto.response.JwtResponse;
import com.utegiftshop.entity.Role;
import com.utegiftshop.entity.User;
import com.utegiftshop.repository.RoleRepository;
import com.utegiftshop.repository.UserRepository;
import com.utegiftshop.security.jwt.JwtTokenProvider;
import com.utegiftshop.security.service.UserDetailsImpl;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(item -> item.getAuthority())
                .orElse(null);

        return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), role));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        User user = new User();
        user.setFullName(signUpRequest.getFullName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));

        Role userRole = roleRepository.findByName("Customer")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        user.setRole(userRole);
        user.setActive(true);
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }
    // --- THÊM API /FORGOT-PASSWORD ---
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam("email") String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (!userOptional.isPresent()) {
            // Trả về lỗi 400 (Bad Request) 
            return ResponseEntity.badRequest().body("Lỗi: Email không tồn tại trong hệ thống.");
        }
        // ---------------------------------------------

        User user = userOptional.get();

        // Tạo mã OTP 6 chữ số (BƯỚC 1: OTP LÀ DÃY SỐ)
        String otp = String.format("%06d", new Random().nextInt(999999));
        
        // Tạo thời gian hết hạn (10 phút)
        long expiryTime = System.currentTimeMillis() + 10 * 60 * 1000;
        
        user.setPasswordResetOtp(otp);
        user.setOtpExpiryTime(new Timestamp(expiryTime));
        
        userRepository.save(user);

        try {
            emailService.sendOtpEmail(email, otp);
            return ResponseEntity.ok("Mã OTP đã được gửi đến email của bạn.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi gửi email: " + e.getMessage());
        }
    }

    // --- THÊM API /RESET-PASSWORD ---
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (!userOptional.isPresent()) {
            return ResponseEntity.badRequest().body("Email không hợp lệ.");
        }

        User user = userOptional.get();

        // Kiểm tra OTP
        if (user.getPasswordResetOtp() == null || !user.getPasswordResetOtp().equals(request.getOtp())) {
            return ResponseEntity.badRequest().body("Mã OTP không chính xác.");
        }

        // Kiểm tra thời gian hết hạn
        if (user.getOtpExpiryTime().before(new Timestamp(System.currentTimeMillis()))) {
            // Xóa OTP cũ
            user.setPasswordResetOtp(null);
            user.setOtpExpiryTime(null);
            userRepository.save(user);
            return ResponseEntity.badRequest().body("Mã OTP đã hết hạn.");
        }

        // Mọi thứ hợp lệ, đổi mật khẩu
        user.setPassword(encoder.encode(request.getNewPassword()));
        
        // Xóa OTP sau khi dùng xong
        user.setPasswordResetOtp(null);
        user.setOtpExpiryTime(null);
        
        userRepository.save(user);

        return ResponseEntity.ok("Đổi mật khẩu thành công.");
    }
}