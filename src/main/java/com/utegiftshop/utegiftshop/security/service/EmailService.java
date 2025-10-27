package com.utegiftshop.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Gửi email chứa mã OTP khôi phục mật khẩu
     * @param toEmail Email người nhận
     * @param otp Mã OTP
     */
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("lengonhuttan2005@gmail.com"); 
            message.setTo(toEmail);
            message.setSubject("[UTE GiftShop] Mã OTP khôi phục mật khẩu của bạn");
            message.setText("Chào bạn,\n\n"
                    + "Mã OTP để khôi phục mật khẩu của bạn là: " + otp + "\n\n"
                    + "Mã này sẽ hết hạn trong 10 phút.\n\n"
                    + "Nếu bạn không yêu cầu, vui lòng bỏ qua email này.\n\n"
                    + "Trân trọng,\n"
                    + "Đội ngũ UTE GiftShop");
            
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }

    /**
     * BỔ SUNG: Gửi email chứa mã OTP kích hoạt tài khoản
     * @param toEmail Email người nhận
     * @param otp Mã OTP
     */
    public void sendActivationOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("okakuri2507@gmail.com"); 
            message.setTo(toEmail);
            message.setSubject("[UTE GiftShop] Mã OTP kích hoạt tài khoản của bạn");
            message.setText("Chào mừng bạn đến với UTE GiftShop,\n\n"
                    + "Mã OTP để kích hoạt tài khoản của bạn là: " + otp + "\n\n"
                    + "Mã này sẽ hết hạn trong 10 phút.\n\n"
                    + "Trân trọng,\n"
                    + "Đội ngũ UTE GiftShop");
            
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }
}