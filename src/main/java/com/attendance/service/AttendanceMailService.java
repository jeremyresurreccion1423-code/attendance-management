package com.attendance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends Attendance OTP mail.
 * Prefer Brevo HTTPS API on Railway (Hobby blocks outbound SMTP).
 * Fall back to Gmail SMTP for local / Pro plans.
 */
@Service
public class AttendanceMailService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceMailService.class);

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${spring.mail.username:}")
    private String configuredUsername;

    @Value("${spring.mail.password:}")
    private String configuredPassword;

    @Value("${attendance.mail.brevo-api-key:}")
    private String brevoApiKey;

    @Value("${attendance.mail.from-name:Attendance Management System}")
    private String fromName;

    public AttendanceMailService(JavaMailSender mailSender, ObjectMapper objectMapper) {
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return hasBrevo() || hasSmtpCredentials();
    }

    public String resolveFromAddress() {
        String fromEnv = firstNonBlank(System.getenv("SPRING_MAIL_USERNAME"), System.getenv("MAIL_USERNAME"));
        if (StringUtils.hasText(fromEnv)) {
            return fromEnv.trim();
        }
        if (StringUtils.hasText(configuredUsername) && !configuredUsername.trim().startsWith("YOUR_")) {
            return configuredUsername.trim();
        }
        if (mailSender instanceof JavaMailSenderImpl impl && StringUtils.hasText(impl.getUsername())) {
            return impl.getUsername().trim();
        }
        return "";
    }

    public void sendText(String to, String subject, String body) throws Exception {
        String from = resolveFromAddress();
        if (!StringUtils.hasText(from) || !from.contains("@")) {
            throw new IllegalStateException("Mail sender email is not configured (spring.mail.username / SPRING_MAIL_USERNAME).");
        }

        if (hasBrevo()) {
            sendViaBrevo(from, to, subject, body);
            return;
        }

        try {
            sendViaSmtp(from, to, subject, body);
        } catch (Exception smtpEx) {
            if (isConnectivityFailure(smtpEx)) {
                throw new IllegalStateException(
                        "Connect timed out: Railway Hobby/Free blocks Gmail SMTP (ports 587/465). "
                                + "Set BREVO_API_KEY in Railway Variables (free HTTPS email), or upgrade Railway to Pro.",
                        smtpEx);
            }
            throw smtpEx;
        }
    }

    private void sendViaSmtp(String from, String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private void sendViaBrevo(String from, String to, String subject, String body) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sender", Map.of("name", fromName, "email", from));
        payload.put("to", List.of(Map.of("email", to)));
        payload.put("subject", subject);
        payload.put("textContent", body);

        String json = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .timeout(Duration.ofSeconds(20))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("api-key", brevoApiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            log.info("OTP email sent via Brevo to {}", to);
            return;
        }
        throw new IllegalStateException("Brevo email failed (" + response.statusCode() + "): "
                + abbreviate(response.body()));
    }

    private boolean hasBrevo() {
        return StringUtils.hasText(brevoApiKey) && !brevoApiKey.trim().startsWith("YOUR_");
    }

    private boolean hasSmtpCredentials() {
        String user = resolveFromAddress();
        String pass = resolveSmtpPassword();
        return StringUtils.hasText(user)
                && user.contains("@")
                && !user.startsWith("YOUR_")
                && pass.length() >= 16
                && !pass.startsWith("YOUR_");
    }

    private String resolveSmtpPassword() {
        String fromEnv = firstNonBlank(System.getenv("SPRING_MAIL_PASSWORD"), System.getenv("MAIL_PASSWORD"));
        if (StringUtils.hasText(fromEnv)) {
            return fromEnv.replace(" ", "").trim();
        }
        if (StringUtils.hasText(configuredPassword) && !configuredPassword.trim().startsWith("YOUR_")) {
            return configuredPassword.replace(" ", "").trim();
        }
        if (mailSender instanceof JavaMailSenderImpl impl && StringUtils.hasText(impl.getPassword())) {
            return impl.getPassword().replace(" ", "").trim();
        }
        return "";
    }

    private static boolean isConnectivityFailure(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String msg = current.getMessage();
            String name = current.getClass().getName();
            if ((msg != null && (msg.toLowerCase().contains("timed out")
                    || msg.toLowerCase().contains("timeout")
                    || msg.toLowerCase().contains("network is unreachable")
                    || msg.toLowerCase().contains("connection refused")))
                    || name.contains("ConnectException")
                    || name.contains("SocketTimeoutException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String abbreviate(String body) {
        if (body == null || body.isBlank()) {
            return "no response body";
        }
        String trimmed = body.trim();
        return trimmed.length() > 180 ? trimmed.substring(0, 177) + "..." : trimmed;
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
