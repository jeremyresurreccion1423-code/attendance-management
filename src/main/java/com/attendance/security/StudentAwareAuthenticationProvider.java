package com.attendance.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

@Slf4j
public class StudentAwareAuthenticationProvider extends DaoAuthenticationProvider {

    private final StudentLoginAccessService studentLoginAccessService;

    public StudentAwareAuthenticationProvider(StudentLoginAccessService studentLoginAccessService) {
        this.studentLoginAccessService = studentLoginAccessService;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
                                                  UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        super.additionalAuthenticationChecks(userDetails, authentication);

        studentLoginAccessService.checkLogin(userDetails.getUsername()).ifPresent(denied -> {
            log.info("Login blocked for {}: {}", userDetails.getUsername(), denied.message());
            throw new StudentAccountInactiveException(denied.message());
        });
    }
}
