package com.attendance.config;

import com.attendance.security.CustomUserDetailsService;
import com.attendance.security.LoginAuthenticationFailureHandler;
import com.attendance.security.StudentAwareAuthenticationProvider;
import com.attendance.security.StudentLoginAccessService;
import com.attendance.security.StudentLoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final StudentLoginAccessService studentLoginAccessService;
    private final LoginAuthenticationFailureHandler loginFailureHandler;
    private final StudentLoginSuccessHandler studentLoginSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        StudentAwareAuthenticationProvider provider = new StudentAwareAuthenticationProvider(studentLoginAccessService);
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return new ProviderManager(List.of(authenticationProvider()));
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
                .requestMatchers("/super-admin/login", "/super-admin/sso").permitAll()
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
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager)
            throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/forgot-password", "/error", "/css/**", "/js/**", "/images/**", "/uploads/**", "/h2-console/**").permitAll()
                .requestMatchers("/api/v1/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/teacher/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/student/**").hasAnyRole("ADMIN", "STUDENT")
                .anyRequest().authenticated()
            )
            .authenticationManager(authenticationManager)
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(studentLoginSuccessHandler)
                .failureHandler(loginFailureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}
