package com.attendance.config;

import com.attendance.service.ProfilePhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class ProfileModelAdvice {

    private final ProfilePhotoService profilePhotoService;

    @ModelAttribute
    public void addProfileAttributes(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            model.addAttribute("profilePhotoUrl", null);
            model.addAttribute("profileInitial", "U");
            return;
        }

        String username = authentication.getName();
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(username));
        model.addAttribute("profileInitial", profilePhotoService.getInitial(username));
    }
}
