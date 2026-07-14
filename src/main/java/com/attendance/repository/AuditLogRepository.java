package com.attendance.repository;

import com.attendance.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop50ByOrderByCreatedAtDesc();

    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    long countByActionAndCreatedAtAfter(String action, java.time.LocalDateTime after);

    List<AuditLog> findByActionAndCreatedAtAfterOrderByCreatedAtDesc(String action, java.time.LocalDateTime after);
}
