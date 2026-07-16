package com.attendance.repository;

import com.attendance.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findBySubjectId(Long subjectId);
    List<Enrollment> findByStudentId(Long studentId);
    Optional<Enrollment> findByStudentIdAndSubjectId(Long studentId, Long subjectId);
    boolean existsByStudentIdAndSubjectId(Long studentId, Long subjectId);
    
    // Optimized query to get enrollment count by subject in single query
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.subject.id = :subjectId")
    long countBySubjectId(@Param("subjectId") Long subjectId);
    
    // Batch query to get all students for multiple subjects (prevents N+1)
    @Query("""
        SELECT DISTINCT e.subject.id, COUNT(e) FROM Enrollment e
        WHERE e.subject.id IN :subjectIds
        GROUP BY e.subject.id
        """)
    List<Object[]> countEnrollmentsBySubjectIds(@Param("subjectIds") List<Long> subjectIds);
}
