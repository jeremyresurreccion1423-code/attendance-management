package com.attendance.controller;

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
public class SuperAdminController {

    @GetMapping("/super-admin")
    public String dashboard(Model model) {
        return "super-admin/dashboard";
    }
}
