package com.attendance.service;

import com.attendance.model.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public void notifyLogin(User user, HttpServletRequest request) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        if (mailFrom == null || mailFrom.isBlank()) {
            return;
        }
        try {
            String ip = AuditService.clientIp(request);
            String ua = request != null ? request.getHeader("User-Agent") : "Unknown";
            String device = summarizeDevice(ua);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(user.getEmail());
            message.setSubject("New Login - Attendance Management System");
            message.setText("""
                    New Login Detected

                    Username: %s
                    Role: %s
                    Device: %s
                    IP Address: %s
                    Time: %s

                    If this was not you, change your password immediately and contact an administrator.
                    """.formatted(
                    user.getUsername(),
                    user.getRole(),
                    device,
                    ip != null ? ip : "Unknown",
                    java.time.LocalDateTime.now()
            ));
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Login notification email failed for {}: {}", user.getUsername(), e.getMessage());
        }
    }

    private static String summarizeDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown device";
        }
        String ua = userAgent.toLowerCase();
        String browser = ua.contains("edg/") ? "Edge"
                : ua.contains("chrome") ? "Chrome"
                : ua.contains("firefox") ? "Firefox"
                : ua.contains("safari") ? "Safari"
                : "Browser";
        String os = ua.contains("windows") ? "Windows"
                : ua.contains("mac") ? "macOS"
                : ua.contains("android") ? "Android"
                : ua.contains("iphone") || ua.contains("ipad") ? "iOS"
                : ua.contains("linux") ? "Linux"
                : "Unknown OS";
        return browser + " on " + os;
    }
}
