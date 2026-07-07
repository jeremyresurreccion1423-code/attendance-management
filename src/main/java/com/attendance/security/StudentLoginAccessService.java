package com.attendance.security;

import com.attendance.model.Role;
import com.attendance.model.Student;
import com.attendance.model.StudentStatus;
import com.attendance.model.Teacher;
import com.attendance.model.TeacherStatus;
import com.attendance.model.User;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.TeacherRepository;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentLoginAccessService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public record AccessDeniedReason(String message) {
    }

    @Transactional(readOnly = true)
    public Optional<AccessDeniedReason> checkLogin(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        User user = userRepository.findByUsername(username.trim()).orElse(null);
        if (user == null) {
            return Optional.empty();
        }

        if (Boolean.FALSE.equals(user.getEnabled())) {
            return Optional.of(new AccessDeniedReason(
                    "Your account has been disabled. Please contact the administrator."));
        }

        if (user.getRole() == Role.STUDENT) {
            return checkStudentLogin(user);
        }

        if (user.getRole() == Role.TEACHER) {
            return checkTeacherLogin(user);
        }

        return Optional.empty();
    }

    private Optional<AccessDeniedReason> checkStudentLogin(User user) {
        Optional<Student> studentOpt = findStudentForUser(user);
        if (studentOpt.isEmpty()) {
            return Optional.empty();
        }

        StudentStatus status = studentOpt.get().getStatus();
        if (status == StudentStatus.ACTIVE) {
            return Optional.empty();
        }

        return Optional.of(new AccessDeniedReason(buildStudentInactiveMessage(status)));
    }

    private Optional<AccessDeniedReason> checkTeacherLogin(User user) {
        Optional<Teacher> teacherOpt = findTeacherForUser(user);
        if (teacherOpt.isEmpty()) {
            return Optional.empty();
        }

        TeacherStatus status = teacherOpt.get().getStatus();
        if (status == TeacherStatus.ACTIVE) {
            return Optional.empty();
        }

        return Optional.of(new AccessDeniedReason(buildTeacherInactiveMessage(status)));
    }

    @Transactional(readOnly = true)
    public Optional<Student> findStudentForUser(User user) {
        if (user == null || user.getId() == null) {
            return Optional.empty();
        }
        Optional<Student> byUserId = studentRepository.findByUserId(user.getId());
        if (byUserId.isPresent()) {
            return byUserId;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return studentRepository.findByStudentNumber(user.getUsername().trim());
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public Optional<Teacher> findTeacherForUser(User user) {
        if (user == null || user.getId() == null) {
            return Optional.empty();
        }
        Optional<Teacher> byUserId = teacherRepository.findByUserId(user.getId());
        if (byUserId.isPresent()) {
            return byUserId;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return teacherRepository.findByEmployeeId(user.getUsername().trim());
        }
        return Optional.empty();
    }

    public String buildStudentInactiveMessage(StudentStatus status) {
        return switch (status) {
            case INACTIVE -> "Your student account is INACTIVE. You cannot log in at this time. Please contact the administrator.";
            case GRADUATED -> "Your student account is marked as GRADUATED. Please contact the administrator if you need access.";
            case DROPPED -> "Your student account is marked as DROPPED. Please contact the administrator if you need access.";
            default -> "Your student account is not active. Please contact the administrator.";
        };
    }

    public String buildTeacherInactiveMessage(TeacherStatus status) {
        return switch (status) {
            case INACTIVE -> "Your teacher account is INACTIVE. You cannot log in at this time. Please contact the administrator.";
            default -> "Your teacher account is not active. Please contact the administrator.";
        };
    }
}
