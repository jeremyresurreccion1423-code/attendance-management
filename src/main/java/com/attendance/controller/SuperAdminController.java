package com.attendance.controller;

import com.attendance.model.User;
import com.attendance.repository.UserRepository;
import com.attendance.service.AccountLockoutService;
import com.attendance.service.SecurityDashboardService;
import com.attendance.service.SuperAdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminDashboardService superAdminDashboardService;
    private final SecurityDashboardService securityDashboardService;
    private final AccountLockoutService accountLockoutService;
    private final UserRepository userRepository;

    @GetMapping("/super-admin")
    public String dashboard(Model model) {
        var combined = superAdminDashboardService.getCombinedDashboard();
        model.addAttribute("attendance", combined.attendance());
        model.addAttribute("library", combined.library());
        model.addAttribute("libraryAvailable", combined.libraryAvailable());
        model.addAttribute("lowAttendanceAlerts", combined.lowAttendanceAlerts());
        return "super-admin/dashboard";
    }

    @GetMapping("/super-admin/create")
    public String create() {
        return "redirect:/admin/create";
    }

    @GetMapping("/super-admin/security")
    public String securityCenter(Model model) {
        var data = securityDashboardService.getDashboard();
        model.addAttribute("sec", data);
        return "super-admin/security";
    }

    @PostMapping("/super-admin/security/unlock/{userId}")
    public String unlockAccount(@PathVariable Long userId,
                                Authentication authentication,
                                RedirectAttributes redirect) {
        User actor = userRepository.findByUsername(authentication.getName()).orElse(null);
        userRepository.findById(userId).ifPresentOrElse(user -> {
            accountLockoutService.unlock(user, actor);
            redirect.addFlashAttribute("message", "Unlocked account: " + user.getUsername());
        }, () -> redirect.addFlashAttribute("error", "User not found"));
        return "redirect:/super-admin/security";
    }
}
