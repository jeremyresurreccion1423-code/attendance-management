package com.attendance.controller;

import com.attendance.config.LibraryAppLinks;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Super Admin dashboard and security pages live on the Library app — redirect all UI routes there.
 */
@Controller
@RequiredArgsConstructor
public class SuperAdminController {

    private final LibraryAppLinks libraryAppLinks;

    @GetMapping("/super-admin")
    public String dashboard() {
        return "redirect:" + libraryAppLinks.superAdminPortal();
    }

    @GetMapping("/super-admin/create")
    public String create() {
        return "redirect:/superadmin/attendance/create";
    }

    @GetMapping("/super-admin/security")
    public String securityCenter() {
        return "redirect:" + libraryAppLinks.superAdminSecurity();
    }

    @PostMapping("/super-admin/security/unlock/{userId}")
    public String unlockAccount(@PathVariable Long userId) {
        return "redirect:" + libraryAppLinks.superAdminSecurity();
    }
}
