package com.utegiftshop.entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(length = 100, nullable = false)
    private String fullName;

    @Column(length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", length = 20, nullable = true)
    private String phoneNumber;

    @Column(name = "avatar_url", length = 512, nullable = true) // Increased length slightly
    private String avatarUrl;

    @Column(length = 255, nullable = false)
    private String password;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "updated_at")
    private Timestamp updatedAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "password_reset_otp")
    private String passwordResetOtp;

    @Column(name = "otp_expiry_time")
    private Timestamp otpExpiryTime;

    @Column(name = "activation_otp")
    private String activationOtp;

    @Column(name = "activation_otp_expiry_time")
    private Timestamp activationOtpExpiryTime;
}