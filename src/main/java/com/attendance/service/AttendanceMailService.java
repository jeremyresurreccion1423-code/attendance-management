package com.attendance.service;

import com.attendance.config.BrevoMailTransport;
import com.attendance.config.CentralMailProperties;
import org.springframework.stereotype.Service;

/**
 * Attendance email sender — Brevo transport (HTTPS on Railway, SMTP fallback).
 */
@Service
public class AttendanceMailService {

    private final BrevoMailTransport brevoMailTransport;
    private final CentralMailProperties centralMailProperties;

    public AttendanceMailService(BrevoMailTransport brevoMailTransport, CentralMailProperties centralMailProperties) {
        this.brevoMailTransport = brevoMailTransport;
        this.centralMailProperties = centralMailProperties;
    }

    public boolean isConfigured() {
        return brevoMailTransport.isConfigured();
    }

    public String resolveFromAddress() {
        return centralMailProperties.getFromHeader();
    }

    public void sendText(String to, String subject, String body) throws Exception {
        brevoMailTransport.sendText(to, subject, body);
    }
}
