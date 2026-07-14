package com.attendance.security;

import com.attendance.model.User;
import com.attendance.repository.UserRepository;
import com.attendance.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditLogoutHandler implements LogoutHandler {

    private final UserRepository userRepository;
    private final AuditService auditService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return;
        }
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        auditService.log(user, "LOGOUT", "User", user != null ? user.getId() : null,
                "User logged out from " + AuditService.clientIp(request));
    }
}
