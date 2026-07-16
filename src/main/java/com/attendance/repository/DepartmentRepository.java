package com.attendance.repository;

import com.attendance.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Department> findByNameIgnoreCase(String name);
    List<Department> findAllByOrderByNameAsc();

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.department.id = :departmentId")
    long countTeachersByDepartmentId(Long departmentId);

    @Query("SELECT COUNT(s) FROM Section s WHERE s.department.id = :departmentId")
    long countSectionsByDepartmentId(Long departmentId);

    @Query("SELECT COUNT(st) FROM Student st WHERE st.department.id = :departmentId")
    long countStudentsByDepartmentId(Long departmentId);

    @Query("""
            SELECT COUNT(sub) FROM Subject sub
            WHERE sub.department.id = :departmentId
              AND sub.subjectCode IS NOT NULL AND TRIM(sub.subjectCode) <> ''
              AND sub.subjectName IS NOT NULL AND TRIM(sub.subjectName) <> ''
              AND sub.teacher IS NOT NULL
              AND sub.section IS NOT NULL
            """)
    long countSubjectsByDepartmentId(Long departmentId);
}
