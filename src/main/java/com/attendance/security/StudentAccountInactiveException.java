package com.attendance.security;

import org.springframework.security.core.AuthenticationException;

public class StudentAccountInactiveException extends AuthenticationException {

    public StudentAccountInactiveException(String message) {
        super(message);
    }
}
