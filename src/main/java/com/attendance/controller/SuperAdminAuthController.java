package com.attendance.controller;

import com.attendance.config.LibraryAppLinks;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Super Admin login is hosted on the Library app only — redirect any visit here to that portal.
 */
@Controller
@RequiredArgsConstructor
public class SuperAdminAuthController {

    private final LibraryAppLinks libraryAppLinks;

    @GetMapping("/super-admin/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout) {
        StringBuilder query = new StringBuilder();
        if (error != null) {
            query.append("error=").append(error);
        }
        if (logout != null) {
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append("logout=").append(logout);
        }
        return "redirect:" + libraryAppLinks.superAdminLoginWithQuery(query.toString());
    }

    @PostMapping("/super-admin/login")
    public String loginPost() {
        return "redirect:" + libraryAppLinks.superAdminLogin();
    }

    @GetMapping("/super-admin/logout")
    public String logoutGet() {
        return "redirect:" + libraryAppLinks.superAdminLogout();
    }

    @PostMapping("/super-admin/logout")
    public String logoutPost() {
        return "redirect:" + libraryAppLinks.superAdminLogout();
    }
}
