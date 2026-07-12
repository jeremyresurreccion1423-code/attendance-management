package com.attendance.model;

public enum Role {
    SUPER_ADMIN,
    ADMIN,
    TEACHER,
    STUDENT;

    public boolean isAdminLevel() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }
}
