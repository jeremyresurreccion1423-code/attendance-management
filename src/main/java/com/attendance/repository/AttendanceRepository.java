package com.attendance.repository;

import com.attendance.model.Attendance;
import com.attendance.model.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findBySubjectIdAndAttendanceDate(Long subjectId, LocalDate date);
    List<Attendance> findBySubjectIdAndAttendanceDateBetween(Long subjectId, LocalDate start, LocalDate end);
    void deleteBySubjectId(Long subjectId);
    List<Attendance> findByStudentId(Long studentId);
    List<Attendance> findByStudentIdAndSubjectId(Long studentId, Long subjectId);
    Optional<Attendance> findByStudentIdAndSubjectIdAndAttendanceDate(Long studentId, Long subjectId, LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.status = :status")
    long countByStudentIdAndStatus(Long studentId, AttendanceStatus status);
    
    // Batch query to get attendance stats by subject and date in single query (prevents N+1)
    @Query("""
        SELECT a.status, COUNT(a) FROM Attendance a
        WHERE a.subject.id IN :subjectIds AND a.attendanceDate = :date
        GROUP BY a.status
        """)
    List<Object[]> getAttendanceStatsForDate(@Param("subjectIds") List<Long> subjectIds, @Param("date") LocalDate date);
    
    // Single query to get all attendance for today for multiple subjects
    @Query("""
        SELECT a FROM Attendance a
        WHERE a.subject.id IN :subjectIds AND a.attendanceDate = :date
        """)
    List<Attendance> findBySubjectIdsAndDate(@Param("subjectIds") List<Long> subjectIds, @Param("date") LocalDate date);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId")
    long countByStudentId(@Param("studentId") Long studentId);

    List<Attendance> findByAttendanceDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.attendanceDate BETWEEN :start AND :end")
    long countByAttendanceDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
        SELECT a.attendanceDate, COUNT(a) FROM Attendance a
        WHERE a.attendanceDate BETWEEN :start AND :end
        GROUP BY a.attendanceDate
        ORDER BY a.attendanceDate
        """)
    List<Object[]> countGroupedByAttendanceDate(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
