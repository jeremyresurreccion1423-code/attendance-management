package com.attendance.repository;

import com.attendance.model.DayOfWeek;
import com.attendance.model.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findBySubjectId(Long subjectId);
    void deleteBySubjectId(Long subjectId);
    List<Timetable> findByTeacherId(Long teacherId);
    List<Timetable> findByPublishedTrue();
    List<Timetable> findByDayOfWeekAndPublishedTrue(DayOfWeek dayOfWeek);
    List<Timetable> findBySubjectIdAndDayOfWeekAndPublishedTrue(Long subjectId, DayOfWeek dayOfWeek);
    
    // Single optimized query to get all timetables for a student (prevents N+1 queries)
    @Query("""
        SELECT DISTINCT t FROM Timetable t
        JOIN t.subject s
        JOIN Enrollment e ON e.subject.id = s.id
        WHERE e.student.id = :studentId
        AND t.published = true
        """)
    List<Timetable> findByStudentId(@Param("studentId") Long studentId);
}
