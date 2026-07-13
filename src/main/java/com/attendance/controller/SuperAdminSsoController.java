package com.attendance.controller;

import com.attendance.model.Role;
import com.attendance.repository.UserRepository;
import com.attendance.security.SsoTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Bridges Super Admin sessions between this app and the Library Management System so a
 * Super Admin who is already authenticated on one system is transparently signed into the
 * other system's Super Admin portal when navigating cross-system links.
 */
@Controller
@RequiredArgsConstructor
public class SuperAdminSsoController {

    private final SsoTokenService ssoTokenService;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    @Value("${attendance.library-app-url}")
    private String libraryAppUrl;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    /** Outbound: generate a fresh handoff token and send the Super Admin into Library's portal. */
    @GetMapping("/super-admin/bridge/library")
    public String bridgeToLibrary(@RequestParam(defaultValue = "/super-admin") String path, Authentication auth) {
        String token = ssoTokenService.generateToken(auth.getName());
        String base = libraryAppUrl.endsWith("/") ? libraryAppUrl.substring(0, libraryAppUrl.length() - 1) : libraryAppUrl;
        String next = URLEncoder.encode(path, StandardCharsets.UTF_8);
        return "redirect:" + base + "/super-admin/sso?token=" + token + "&next=" + next;
    }

    /** Inbound: consume a handoff token issued by Library and sign the Super Admin into this app. */
    @GetMapping("/super-admin/sso")
    public String receiveSso(@RequestParam String token,
                             @RequestParam(defaultValue = "/super-admin") String next,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        var usernameOpt = ssoTokenService.validateToken(token);
        if (usernameOpt.isEmpty()) {
            return "redirect:/super-admin/login?error=true";
        }
        String username = usernameOpt.get();
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || userOpt.get().getRole() != Role.SUPER_ADMIN || !Boolean.TRUE.equals(userOpt.get().getEnabled())) {
            return "redirect:/super-admin/login?error=true";
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        String safeNext = isSafeLocalPath(next) ? next : "/super-admin";
        return "redirect:" + safeNext;
    }

    /** Only allow same-app relative paths as SSO redirect targets (blocks open-redirect to external hosts). */
    private boolean isSafeLocalPath(String path) {
        return path != null && path.startsWith("/") && !path.startsWith("//");
    }
}
