package com.attendance.config;

import com.attendance.security.AuditLogoutHandler;
import com.attendance.security.CustomUserDetailsService;
import com.attendance.security.LoginAuthenticationFailureHandler;
import com.attendance.security.LoginAuthenticationSuccessHandler;
import com.attendance.security.StudentAwareAuthenticationProvider;
import com.attendance.security.StudentLoginAccessService;
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
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final StudentLoginAccessService studentLoginAccessService;
    private final LoginAuthenticationSuccessHandler loginSuccessHandler;
    private final LoginAuthenticationFailureHandler loginFailureHandler;
    private final AuditLogoutHandler auditLogoutHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        StudentAwareAuthenticationProvider provider =
                new StudentAwareAuthenticationProvider(studentLoginAccessService);
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
                            + "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://unpkg.com; "
                            + "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; "
                            + "img-src 'self' data: blob:; "
                            + "font-src 'self' data: https://cdn.jsdelivr.net https://fonts.gstatic.com; "
                            + "connect-src 'self'; "
                            + "frame-ancestors 'self'"));
    }

    @Bean
    @Order(1)
    public SecurityFilterChain superAdminChain(HttpSecurity http) throws Exception {
        http.securityMatcher(new OrRequestMatcher(
                new AntPathRequestMatcher("/super-admin/**"),
                new AntPathRequestMatcher("/superadmin/**")));

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/super-admin/login",
                        "/super-admin/login/process",
                        "/super-admin/sso",
                        "/super-admin/bridge/**",
                        "/css/**",
                        "/js/**",
                        "/images/**")
                .permitAll()
                .anyRequest().hasRole("SUPER_ADMIN"))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/super-admin/login")))
            .logout(logout -> logout
                .logoutUrl("/super-admin/logout")
                .addLogoutHandler(auditLogoutHandler)
                .logoutSuccessUrl("/super-admin/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll())
            .headers(this::applySecurityHeaders);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
        http.securityMatcher(new AntPathRequestMatcher("/admin/**"));

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
                .permitAll())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .addLogoutHandler(auditLogoutHandler)
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll())
            .authenticationProvider(authenticationProvider())
            .headers(this::applySecurityHeaders);

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(new NegatedRequestMatcher(
                new OrRequestMatcher(
                        new AntPathRequestMatcher("/super-admin/**"),
                        new AntPathRequestMatcher("/superadmin/**"),
                        new AntPathRequestMatcher("/admin/**"))));

        http
            .csrf(csrf -> csrf.disable())
            .headers(this::applySecurityHeaders)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/forgot-password", "/forgot-password/**", "/error", "/css/**", "/js/**", "/images/**", "/uploads/**", "/h2-console/**").permitAll()
                .requestMatchers("/api/v1/super-admin/dashboard-stats").permitAll()
                .requestMatchers("/api/v1/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
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
