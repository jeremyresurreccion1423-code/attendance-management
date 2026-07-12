package com.attendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Dedicated login page for the Super Admin role — kept completely separate from the
 * regular Admin/Teacher/Student login so Super Admin authentication never mixes with
 * end-user flows.
 */
@Controller
public class SuperAdminAuthController {

    @GetMapping("/super-admin/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Model model) {
        if (error != null) model.addAttribute("error", "Invalid Super Admin credentials");
        if (logout != null) model.addAttribute("message", "You have been logged out of the System Control Center");
        return "super-admin/login";
    }
}
