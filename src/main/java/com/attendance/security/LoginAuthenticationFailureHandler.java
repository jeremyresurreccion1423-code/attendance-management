package com.attendance.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class LoginAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public LoginAuthenticationFailureHandler() {
        super("/login?error=true");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        if (exception instanceof StudentAccountInactiveException inactive) {
            String message = inactive.getMessage() != null ? inactive.getMessage()
                    : "Your student account is INACTIVE. Please contact the administrator.";
            String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
            getRedirectStrategy().sendRedirect(request, response, "/login?error=inactive&message=" + encoded);
            return;
        }
        if (exception instanceof DisabledException) {
            getRedirectStrategy().sendRedirect(request, response, "/login?error=disabled");
            return;
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}
