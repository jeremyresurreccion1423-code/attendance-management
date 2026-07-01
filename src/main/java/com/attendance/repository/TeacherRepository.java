package com.attendance.repository;

import com.attendance.model.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByEmployeeId(String employeeId);
    Optional<Teacher> findByUserId(Long userId);
    boolean existsByEmployeeId(String employeeId);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Teacher> findByDepartmentIdOrderByFullNameAsc(Long departmentId);
    Page<Teacher> findByDepartmentId(Long departmentId, Pageable pageable);
    Page<Teacher> findAllByOrderByFullNameAsc(Pageable pageable);
}
