package com.attendance.service;

import com.attendance.model.FaceRecognitionLog;
import com.attendance.model.FaceRecognitionResult;
import com.attendance.model.Student;
import com.attendance.model.Subject;
import com.attendance.repository.FaceRecognitionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FaceRecognitionService {

    private final FaceRecognitionLogRepository logRepository;

    @Transactional
    public FaceRecognitionLog log(Student student, Subject subject, FaceRecognitionResult result, double confidence) {
        return logRepository.save(FaceRecognitionLog.builder()
                .student(student)
                .subject(subject)
                .result(result)
                .confidenceScore(confidence)
                .build());
    }

    /**
     * Simulated face verification. In production, integrate OpenCV or a cloud API.
     * Returns true if confidence >= 0.85 (demo accepts any non-empty student ID hash).
     */
    public boolean verify(Long studentId, String faceData) {
        if (faceData == null || faceData.isBlank()) {
            return false;
        }
        // Demo: deterministic match based on student ID presence in face data token
        double confidence = faceData.contains(String.valueOf(studentId)) ? 0.95 : 0.45;
        return confidence >= 0.85;
    }
}
