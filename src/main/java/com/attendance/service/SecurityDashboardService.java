package com.attendance.service;

import com.attendance.model.AuditLog;
import com.attendance.model.User;
import com.attendance.repository.AuditLogRepository;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityDashboardService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AccountLockoutService accountLockoutService;

    @Transactional(readOnly = true)
    public SecurityDashboardData getDashboard() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        long todayLogins = auditLogRepository.countByActionAndCreatedAtAfter("LOGIN", startOfDay);
        long todayFailed = auditLogRepository.countByActionAndCreatedAtAfter("LOGIN_FAILED", startOfDay);
        long todayLockouts = auditLogRepository.countByActionAndCreatedAtAfter("ACCOUNT_LOCKED", startOfDay);
        List<User> locked = accountLockoutService.findCurrentlyLocked();
        List<AuditLog> recent = auditLogRepository.findTop100ByOrderByCreatedAtDesc();
        List<AuditLog> recentFailed = auditLogRepository
                .findByActionAndCreatedAtAfterOrderByCreatedAtDesc("LOGIN_FAILED", startOfDay.minusDays(7));

        return new SecurityDashboardData(
                todayLogins,
                todayFailed,
                todayLockouts,
                locked.size(),
                locked,
                recent,
                recentFailed.size() > 50 ? recentFailed.subList(0, 50) : recentFailed,
                startOfDay,
                endOfDay
        );
    }

    public record SecurityDashboardData(
            long todayLogins,
            long todayFailedLogins,
            long todayLockouts,
            long lockedAccountsCount,
            List<User> lockedAccounts,
            List<AuditLog> recentAuditLogs,
            List<AuditLog> recentFailedLogins,
            LocalDateTime dayStart,
            LocalDateTime dayEnd
    ) {}
}
