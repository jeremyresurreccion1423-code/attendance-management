package com.attendance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

import java.util.Properties;

/**
 * Explicit Gmail SMTP sender for Attendance OTP.
 * Prefers spring.mail.* (application-local / Railway SPRING_MAIL_*),
 * then falls back to MAIL_USERNAME / MAIL_PASSWORD env vars.
 */
@Configuration
public class MailConfiguration {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String configuredUsername;

    @Value("${spring.mail.password:}")
    private String configuredPassword;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(resolveUsername());
        mailSender.setPassword(resolvePassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");
        return mailSender;
    }

    private String resolveUsername() {
        // Env wins so Railway variables override any credentials baked into the jar
        String fromEnv = firstNonBlank(System.getenv("SPRING_MAIL_USERNAME"), System.getenv("MAIL_USERNAME"));
        if (StringUtils.hasText(fromEnv)) {
            return fromEnv.trim();
        }
        if (StringUtils.hasText(configuredUsername) && !configuredUsername.trim().startsWith("YOUR_")) {
            return configuredUsername.trim();
        }
        return "";
    }

    private String resolvePassword() {
        String fromEnv = firstNonBlank(System.getenv("SPRING_MAIL_PASSWORD"), System.getenv("MAIL_PASSWORD"));
        if (StringUtils.hasText(fromEnv)) {
            return fromEnv.replace(" ", "").trim();
        }
        if (StringUtils.hasText(configuredPassword) && !configuredPassword.trim().startsWith("YOUR_")) {
            return configuredPassword.replace(" ", "").trim();
        }
        return "";
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a;
        }
        if (StringUtils.hasText(b)) {
            return b;
        }
        return null;
    }
}
