package com.attendance.controller.superadmin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Keeps Super Admin users inside the /superadmin/attendance module — never on /admin/* pages.
 */
@Component
@Order(50)
public class SuperAdminRedirectFilter extends OncePerRequestFilter {

    private static final Map<String, String> ADMIN_TO_SUPERADMIN = Map.ofEntries(
            Map.entry("/admin", "/superadmin/attendance/dashboard"),
            Map.entry("/admin/", "/superadmin/attendance/dashboard"),
            Map.entry("/admin/dashboard", "/superadmin/attendance/dashboard"),
            Map.entry("/admin/departments", "/superadmin/attendance/departments"),
            Map.entry("/admin/create", "/superadmin/attendance/create"),
            Map.entry("/admin/students", "/superadmin/attendance/students"),
            Map.entry("/admin/teachers", "/superadmin/attendance/teachers"),
            Map.entry("/admin/subjects", "/superadmin/attendance/subjects"),
            Map.entry("/admin/sections", "/superadmin/attendance/sections"),
            Map.entry("/admin/reports", "/superadmin/attendance/reports"),
            Map.entry("/admin/trends", "/superadmin/attendance/trends")
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("GET".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI() != null
                && request.getRequestURI().startsWith("/admin")
                && isSuperAdmin()) {
            String target = resolveTarget(request.getRequestURI());
            if (target != null) {
                String qs = request.getQueryString();
                response.sendRedirect(target + (qs != null && !qs.isBlank() ? "?" + qs : ""));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private static String resolveTarget(String uri) {
        if (ADMIN_TO_SUPERADMIN.containsKey(uri)) {
            return ADMIN_TO_SUPERADMIN.get(uri);
        }
        return null;
    }

    private static boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }
}
