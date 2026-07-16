package com.attendance.repository;

import com.attendance.model.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findBySubjectCode(String subjectCode);
    List<Subject> findByTeacherId(Long teacherId);
    List<Subject> findBySectionId(Long sectionId);
    boolean existsBySubjectCode(String subjectCode);

    List<Subject> findByDepartmentIdOrderBySubjectNameAsc(Long departmentId);

    /**
     * Subjects for a department, including legacy rows whose department_id is null
     * but whose section/teacher already belongs to that department.
     */
    @Query("""
            SELECT DISTINCT s FROM Subject s
            LEFT JOIN FETCH s.department
            LEFT JOIN FETCH s.teacher t
            LEFT JOIN FETCH t.department
            LEFT JOIN FETCH s.section sec
            LEFT JOIN FETCH sec.department
            WHERE s.subjectCode IS NOT NULL AND TRIM(s.subjectCode) <> ''
              AND s.subjectName IS NOT NULL AND TRIM(s.subjectName) <> ''
              AND (
                    (s.department IS NOT NULL AND s.department.id = :departmentId)
                 OR (sec IS NOT NULL AND sec.department IS NOT NULL AND sec.department.id = :departmentId)
                 OR (t IS NOT NULL AND t.department IS NOT NULL AND t.department.id = :departmentId)
              )
            """)
    List<Subject> findAllForDepartment(@Param("departmentId") Long departmentId);

    Page<Subject> findByDepartmentId(Long departmentId, Pageable pageable);
    Page<Subject> findAllByOrderBySubjectNameAsc(Pageable pageable);
    long countByTeacherId(Long teacherId);
    boolean existsBySubjectCodeAndIdNot(String subjectCode, Long id);

    /** Incomplete / ghost rows that inflate department counts but are hidden from the Subjects UI. */
    @Query("""
            SELECT s FROM Subject s
            WHERE s.department.id = :departmentId
              AND (
                    s.teacher IS NULL
                 OR s.section IS NULL
                 OR s.subjectCode IS NULL OR TRIM(s.subjectCode) = ''
                 OR s.subjectName IS NULL OR TRIM(s.subjectName) = ''
              )
            """)
    List<Subject> findIncompleteByDepartmentId(@Param("departmentId") Long departmentId);
}
