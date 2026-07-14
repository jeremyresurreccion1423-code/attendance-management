package com.attendance.service;

import com.attendance.config.CentralMailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.MimeMessage;

/**
 * Attendance email sender — Brevo SMTP only, centralized LU From identity.
 */
@Service
public class AttendanceMailService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceMailService.class);

    private final JavaMailSender mailSender;
    private final CentralMailProperties centralMailProperties;

    @Value("${spring.mail.password:}")
    private String configuredPassword;

    public AttendanceMailService(JavaMailSender mailSender, CentralMailProperties centralMailProperties) {
        this.mailSender = mailSender;
        this.centralMailProperties = centralMailProperties;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(resolveSmtpPassword())
                && StringUtils.hasText(centralMailProperties.getFromEmail());
    }

    public String resolveFromAddress() {
        return centralMailProperties.getFromHeader();
    }

    public void sendText(String to, String subject, String body) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Mail is not configured. Set MAIL_PASSWORD (or BREVO_SMTP_PASSWORD) for Brevo SMTP.");
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
        helper.setFrom(centralMailProperties.getFromEmail(), centralMailProperties.getFromName());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, false);
        mailSender.send(mimeMessage);
        log.info("Email sent via Brevo SMTP to {} from {}", to, centralMailProperties.getFromHeader());
    }

    /** Convenience overload kept for SimpleMailMessage call sites if needed. */
    public void sendSimpleText(String to, String subject, String body) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Mail is not configured. Set MAIL_PASSWORD (or BREVO_SMTP_PASSWORD) for Brevo SMTP.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(centralMailProperties.getFromHeader());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private String resolveSmtpPassword() {
        String fromEnv = firstNonBlank(
                System.getenv("MAIL_PASSWORD"),
                System.getenv("BREVO_SMTP_PASSWORD"),
                System.getenv("SPRING_MAIL_PASSWORD"));
        if (StringUtils.hasText(fromEnv)) {
            return fromEnv.replace(" ", "").trim();
        }
        if (StringUtils.hasText(configuredPassword)) {
            return configuredPassword.replace(" ", "").trim();
        }
        if (mailSender instanceof JavaMailSenderImpl impl && StringUtils.hasText(impl.getPassword())) {
            return impl.getPassword().replace(" ", "").trim();
        }
        return "";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
