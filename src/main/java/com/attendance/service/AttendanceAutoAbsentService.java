package com.attendance.service;

import com.attendance.model.*;
import com.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceAutoAbsentService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceAutoAbsentService.class);

    private final AttendanceQRRepository qrRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;

    @Scheduled(fixedRate = 300_000)
    public void scheduledAutoAbsent() {
        processForDate(LocalDate.now());
    }

    @Transactional
    public int processForSubject(Long subjectId, LocalDate date) {
        List<AttendanceQR> sessions = qrRepository.findBySubjectIdAndSessionDate(subjectId, date);
        int marked = 0;
        for (AttendanceQR qr : sessions) {
            marked += processQrSession(qr, date);
        }
        return marked;
    }

    @Transactional
    public int processForDate(LocalDate date) {
        int marked = 0;
        for (AttendanceQR qr : qrRepository.findBySessionDate(date)) {
            marked += processQrSession(qr, date);
        }
        if (marked > 0) {
            log.info("Auto-marked {} student(s) absent for {}", marked, date);
        }
        return marked;
    }

    private int processQrSession(AttendanceQR qr, LocalDate date) {
        if (qr.getExpiresAt() == null || qr.getExpiresAt().isAfter(LocalDateTime.now())) {
            return 0;
        }

        Long subjectId = qr.getSubject().getId();
        int marked = 0;
        for (Enrollment enrollment : enrollmentRepository.findBySubjectId(subjectId)) {
            Student student = enrollment.getStudent();
            boolean hasRecord = attendanceRepository
                    .findByStudentIdAndSubjectIdAndAttendanceDate(student.getId(), subjectId, date)
                    .isPresent();
            if (!hasRecord) {
                attendanceService.markAutoAbsent(student.getId(), subjectId, date,
                        "Auto-marked absent: no QR scan before the session expired");
                marked++;
            }
        }

        if (Boolean.TRUE.equals(qr.getActive())) {
            qr.setActive(false);
            qrRepository.save(qr);
        }

        return marked;
    }
}
