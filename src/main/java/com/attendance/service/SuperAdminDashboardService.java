package com.attendance.service;

import com.attendance.dto.SuperAdminCombinedDashboard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class SuperAdminDashboardService {

    private final DashboardService dashboardService;
    private final RestTemplate restTemplate;
    private final String libraryAppUrl;
    private final String ssoSecret;

    public SuperAdminDashboardService(
            DashboardService dashboardService,
            RestTemplate restTemplate,
            @Value("${attendance.library-app-url}") String libraryAppUrl,
            @Value("${super-admin.sso-secret}") String ssoSecret) {
        this.dashboardService = dashboardService;
        this.restTemplate = restTemplate;
        this.libraryAppUrl = libraryAppUrl;
        this.ssoSecret = ssoSecret;
    }

    public SuperAdminCombinedDashboard getCombinedDashboard() {
        Map<String, Object> attendance = dashboardService.getAdminDashboard();
        Map<String, Object> library = fetchLibraryStats();
        boolean libraryAvailable = library.get("available") == Boolean.TRUE;
        return SuperAdminCombinedDashboard.of(attendance, library, libraryAvailable);
    }

    public Map<String, Object> getAttendanceStatsForApi() {
        Map<String, Object> stats = new HashMap<>(dashboardService.getAdminDashboard());
        stats.put("system", "attendance");
        return stats;
    }

    private Map<String, Object> fetchLibraryStats() {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("available", false);
        fallback.put("bookCount", 0);
        fallback.put("studentCount", 0);
        fallback.put("activeLoans", 0);
        fallback.put("overdueLoans", 0);

        try {
            String base = normalizeBaseUrl(libraryAppUrl);
            if (base == null) {
                return fallback;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Super-Admin-Secret", ssoSecret);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    base + "/api/v1/super-admin/dashboard-stats",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {});
            if (response.getBody() == null) {
                return fallback;
            }
            Map<String, Object> body = new HashMap<>(response.getBody());
            body.put("available", true);
            return body;
        } catch (Exception ex) {
            return fallback;
        }
    }

    /** Accepts host-only Railway values and makes them absolute https URLs. */
    static String normalizeBaseUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String base = raw.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "https://" + base;
        }
        return base;
    }
}
