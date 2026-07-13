package com.attendance.controller;

import com.attendance.service.SuperAdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Centralized Super Admin dashboard. Provides a single hub of links into every
 * admin-only feature of the Attendance Management System (native, same app) and
 * every admin-only feature of the Library Management System (via SSO bridge links).
 * No student, teacher, or other end-user functionality is exposed here.
 */
@Controller
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminDashboardService superAdminDashboardService;

    @GetMapping("/super-admin")
    public String dashboard(Model model) {
        var combined = superAdminDashboardService.getCombinedDashboard();
        model.addAttribute("attendance", combined.attendance());
        model.addAttribute("library", combined.library());
        model.addAttribute("libraryAvailable", combined.libraryAvailable());
        model.addAttribute("lowAttendanceAlerts", combined.lowAttendanceAlerts());
        return "super-admin/dashboard";
    }
}
