package com.attendance.repository;

import com.attendance.model.FaceRecognitionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaceRecognitionLogRepository extends JpaRepository<FaceRecognitionLog, Long> {
    List<FaceRecognitionLog> findByStudentId(Long studentId);
}
