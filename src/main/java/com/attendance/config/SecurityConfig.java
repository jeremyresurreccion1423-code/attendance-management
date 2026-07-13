package com.attendance.config;

import com.attendance.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

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

    /** Dedicated Super Admin portal: its own login page and authentication flow, isolated from the regular login. */
    @Bean
    @Order(1)
    public SecurityFilterChain superAdminChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/super-admin/**")
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/super-admin/login", "/super-admin/sso", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().hasRole("SUPER_ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/super-admin/login")
                .loginProcessingUrl("/super-admin/login")
                .failureUrl("/super-admin/login?error=true")
                .successHandler((request, response, authentication) -> {
                    boolean isSuperAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
                    if (!isSuperAdmin) {
                        new SecurityContextLogoutHandler().logout(request, response, authentication);
                        response.sendRedirect("/super-admin/login?error=true");
                        return;
                    }
                    response.sendRedirect("/super-admin");
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/super-admin/logout")
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
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/forgot-password", "/error", "/css/**", "/js/**", "/images/**", "/uploads/**", "/h2-console/**").permitAll()
                .requestMatchers("/api/v1/super-admin/dashboard-stats").permitAll()
                .requestMatchers("/api/v1/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/teacher/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/student/**").hasAnyRole("ADMIN", "STUDENT")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .failureUrl("/login?error=true")
                .successHandler((request, response, authentication) -> {
                    boolean isSuperAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
                    if (isSuperAdmin) {
                        new SecurityContextLogoutHandler().logout(request, response, authentication);
                        response.sendRedirect("/login?superAdmin=true");
                        return;
                    }
                    response.sendRedirect("/dashboard");
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
