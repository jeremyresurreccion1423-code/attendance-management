package com.attendance.repository;

import com.attendance.model.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByEmployeeId(String employeeId);
    Optional<Teacher> findByUserId(Long userId);
    boolean existsByEmployeeId(String employeeId);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Teacher> findByDepartmentIdOrderByFullNameAsc(Long departmentId);

    /**
     * Teachers for a department, including those whose department_id is missing/wrong
     * but who already teach subjects belonging to that department.
     */
    @Query("""
            SELECT DISTINCT t FROM Teacher t
            LEFT JOIN FETCH t.department
            LEFT JOIN Subject s ON s.teacher = t
            LEFT JOIN s.department sd
            WHERE t.fullName IS NOT NULL AND TRIM(t.fullName) <> ''
              AND (
                    (t.department IS NOT NULL AND t.department.id = :departmentId)
                 OR (sd IS NOT NULL AND sd.id = :departmentId)
              )
            """)
    List<Teacher> findAllForDepartment(@Param("departmentId") Long departmentId);

    Page<Teacher> findByDepartmentId(Long departmentId, Pageable pageable);
    Page<Teacher> findAllByOrderByFullNameAsc(Pageable pageable);
}
