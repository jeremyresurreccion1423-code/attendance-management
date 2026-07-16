package com.attendance.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class StudentLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final StudentLoginAccessService studentLoginAccessService;

    @PostConstruct
    void init() {
        setDefaultTargetUrl("/dashboard");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        var denied = studentLoginAccessService.checkLogin(authentication.getName());
        if (denied.isPresent()) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            String encoded = URLEncoder.encode(denied.get().message(), StandardCharsets.UTF_8);
            getRedirectStrategy().sendRedirect(request, response, "/login?error=inactive&message=" + encoded);
            return;
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
