package com.attendance.repository;

import com.attendance.model.AttendanceQR;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceQRRepository extends JpaRepository<AttendanceQR, Long> {
    Optional<AttendanceQR> findByQrCodeAndActiveTrue(String qrCode);
    List<AttendanceQR> findBySubjectIdAndSessionDate(Long subjectId, LocalDate sessionDate);
    List<AttendanceQR> findBySessionDate(LocalDate sessionDate);
    void deleteByTimetable_Id(Long timetableId);
}
