package com.attendance.service;

import com.attendance.model.*;
import com.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByUsernameOrEmail(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String key = identifier.trim();
        return userRepository.findByUsername(key)
                .or(() -> userRepository.findByEmailIgnoreCase(key));
    }

    @Transactional
    public void updateLastLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Transactional
    public boolean resetPassword(String username, String newPassword) {
        return userRepository.findByUsername(username).map(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            logAction(user, "PASSWORD_RESET", "User", user.getId(), "Password reset requested");
            return true;
        }).orElse(false);
    }

    public boolean verifyPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Transactional
    public void changePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logAction(user, "PASSWORD_CHANGE", "User", user.getId(), "Password changed by user");
    }

    @Transactional
    public User createUser(String username, String password, Role role) {
        return createUser(username, password, role, null, null);
    }

    @Transactional
    public User createUser(String username, String password, Role role, String email, String fullName) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (email != null && !email.isBlank() && userRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("Email already exists");
        }
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .email(email != null && !email.isBlank() ? email.trim().toLowerCase() : null)
                .fullName(fullName != null && !fullName.isBlank() ? fullName.trim() : null)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    public void logAction(User user, String action, String entityType, Long entityId, String details) {
        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build());
    }
}
