package com.attendance.config;

import com.attendance.service.SuperAdminDashboardService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Absolute URLs for the Library Management System — Super Admin login and portal live there only.
 */
@Component
public class LibraryAppLinks {

    @Value("${attendance.library-app-url}")
    private String libraryAppUrl;

    public String baseUrl() {
        return SuperAdminDashboardService.normalizeBaseUrl(libraryAppUrl);
    }

    public String superAdminLogin() {
        String base = baseUrl();
        return base != null ? base + "/super-admin/login" : "/login";
    }

    public String superAdminPortal() {
        String base = baseUrl();
        return base != null ? base + "/super-admin" : "/login";
    }

    public String superAdminSecurity() {
        String base = baseUrl();
        return base != null ? base + "/super-admin/security" : "/login";
    }

    public String superAdminLogout() {
        String base = baseUrl();
        return base != null ? base + "/super-admin/logout" : "/login?logout=true";
    }

    public String superAdminLoginWithQuery(String query) {
        if (query == null || query.isBlank()) {
            return superAdminLogin();
        }
        String q = query.startsWith("?") ? query : "?" + query;
        return superAdminLogin() + q;
    }
}
