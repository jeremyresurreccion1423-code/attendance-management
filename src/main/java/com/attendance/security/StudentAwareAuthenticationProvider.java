package com.attendance.security;

import com.attendance.model.Role;
import com.attendance.model.Student;
import com.attendance.model.StudentStatus;
import com.attendance.model.User;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

@Slf4j
public class StudentAwareAuthenticationProvider extends DaoAuthenticationProvider {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public StudentAwareAuthenticationProvider(UserRepository userRepository,
                                              StudentRepository studentRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
                                                  UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        super.additionalAuthenticationChecks(userDetails, authentication);

        User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return;
        }

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new StudentAccountInactiveException(
                    "Your account has been disabled. Please contact the administrator.");
        }

        if (user.getRole() != Role.STUDENT) {
            return;
        }

        Optional<Student> studentOpt = studentRepository.findByUserId(user.getId());
        if (studentOpt.isEmpty()) {
            return;
        }

        StudentStatus status = studentOpt.get().getStatus();
        if (status == StudentStatus.ACTIVE) {
            return;
        }

        log.info("Login blocked for inactive student {} (status={})", user.getUsername(), status);
        throw new StudentAccountInactiveException(buildInactiveMessage(status));
    }

    private String buildInactiveMessage(StudentStatus status) {
        return switch (status) {
            case INACTIVE -> "Your student account is INACTIVE. You cannot log in at this time. Please contact the administrator.";
            case GRADUATED -> "Your student account is marked as GRADUATED. Please contact the administrator if you need access.";
            case DROPPED -> "Your student account is marked as DROPPED. Please contact the administrator if you need access.";
            default -> "Your student account is not active. Please contact the administrator.";
        };
    }
}
