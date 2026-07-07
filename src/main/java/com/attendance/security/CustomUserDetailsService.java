package com.attendance.security;

import com.attendance.model.Role;
import com.attendance.model.User;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.TeacherRepository;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String key = identifier == null ? "" : identifier.trim();
        if (key.isEmpty()) {
            throw new UsernameNotFoundException("Empty login identifier");
        }

        Optional<User> user = userRepository.findByUsername(key);
        if (user.isEmpty()) {
            user = userRepository.findByEmailIgnoreCase(key);
        }
        if (user.isEmpty()) {
            user = studentRepository.findByStudentNumber(key).map(student -> student.getUser());
        }
        if (user.isEmpty()) {
            user = teacherRepository.findByEmployeeId(key).map(teacher -> teacher.getUser());
        }

        User resolved = user.orElseThrow(() -> new UsernameNotFoundException("User not found: " + key));

        boolean accountEnabled = Boolean.TRUE.equals(resolved.getEnabled());
        if (resolved.getRole() == Role.STUDENT || resolved.getRole() == Role.TEACHER) {
            // Validate role status after password check in StudentAwareAuthenticationProvider.
            accountEnabled = true;
        }

        return new org.springframework.security.core.userdetails.User(
                resolved.getUsername(),
                resolved.getPassword(),
                accountEnabled,
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + resolved.getRole().name()))
        );
    }
}
