package com.attendance.config;

import com.attendance.security.AuditLogoutHandler;
import com.attendance.security.CustomUserDetailsService;
import com.attendance.security.LoginAuthenticationFailureHandler;
import com.attendance.security.LoginAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final LoginAuthenticationSuccessHandler loginSuccessHandler;
    private final LoginAuthenticationFailureHandler loginFailureHandler;
    private final AuditLogoutHandler auditLogoutHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    private void applySecurityHeaders(HeadersConfigurer<HttpSecurity> headers) {
        headers
            .frameOptions(frame -> frame.sameOrigin())
            .contentTypeOptions(contentType -> {})
            .referrerPolicy(referrer -> referrer.policy(
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; "
                            + "script-src 'self' 'unsafe-inline'; "
                            + "style-src 'self' 'unsafe-inline'; "
                            + "img-src 'self' data: blob:; "
                            + "font-src 'self' data:; "
                            + "connect-src 'self'; "
                            + "frame-ancestors 'self'"));
    }

    @Bean
    @Order(1)
    public SecurityFilterChain superAdminChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/super-admin/**")
            .csrf(csrf -> csrf.disable())
            .headers(this::applySecurityHeaders)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/super-admin/login", "/super-admin/sso", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().hasRole("SUPER_ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/super-admin/login")
                .loginProcessingUrl("/super-admin/login")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/super-admin/logout")
                .addLogoutHandler(auditLogoutHandler)
                .logoutSuccessUrl("/super-admin/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(this::applySecurityHeaders)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/forgot-password", "/forgot-password/**", "/error", "/css/**", "/js/**", "/images/**", "/uploads/**", "/h2-console/**").permitAll()
                .requestMatchers("/api/v1/super-admin/dashboard-stats").permitAll()
                .requestMatchers("/api/v1/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/super-admin/security/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/teacher/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/student/**").hasAnyRole("ADMIN", "STUDENT")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .addLogoutHandler(auditLogoutHandler)
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
