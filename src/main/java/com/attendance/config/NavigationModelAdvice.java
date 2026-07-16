package com.attendance.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
@RequiredArgsConstructor
public class NavigationModelAdvice {

    private final LibraryAppLinks libraryAppLinks;

    @ModelAttribute
    public void addNavigationAttributes(HttpServletRequest request, Model model) {
        String uri = request != null ? request.getRequestURI() : "";
        model.addAttribute("navUri", uri);
        model.addAttribute("navAttOpen", uri.startsWith("/superadmin/attendance"));
        model.addAttribute("libraryControlCenterUrl", libraryAppLinks.superAdminPortal());
    }
}
