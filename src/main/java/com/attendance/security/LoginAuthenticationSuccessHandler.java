package com.attendance.security;

import com.attendance.config.LibraryAppLinks;
import com.attendance.model.User;
import com.attendance.repository.UserRepository;
import com.attendance.service.AccountLockoutService;
import com.attendance.service.AuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AccountLockoutService accountLockoutService;
    private final AuditService auditService;
    private final LibraryAppLinks libraryAppLinks;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        boolean isSuperAdminPortal = request.getRequestURI() != null
                && request.getRequestURI().startsWith("/super-admin");
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user != null) {
            accountLockoutService.onSuccessfulLogin(user);
            auditService.log(user, "LOGIN", "User", user.getId(),
                    (isSuperAdminPortal ? "Super Admin portal login (redirected)" : "User login")
                            + " from " + AuditService.clientIp(request));
        }

        if (isSuperAdminPortal || isSuperAdmin) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            response.sendRedirect(libraryAppLinks.superAdminLogin());
            return;
        }
        response.sendRedirect("/dashboard");
    }
}
