package com.attendance.repository;

import com.attendance.model.Student;
import com.attendance.model.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentNumber(String studentNumber);
    Optional<Student> findByUserId(Long userId);
    List<Student> findByStatus(StudentStatus status);
    List<Student> findBySectionId(Long sectionId);
    boolean existsByStudentNumber(String studentNumber);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Student> findByDepartmentIdOrderByFullNameAsc(Long departmentId);
    List<Student> findByDepartmentIdAndYearLevelOrderByFullNameAsc(Long departmentId, String yearLevel);
    List<Student> findByDepartmentIdAndYearLevelAndSectionIdOrderByFullNameAsc(Long departmentId, String yearLevel, Long sectionId);

    Page<Student> findByDepartmentId(Long departmentId, Pageable pageable);
    Page<Student> findAllByOrderByFullNameAsc(Pageable pageable);
}
