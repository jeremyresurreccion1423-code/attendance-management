package com.attendance.model;

public enum Role {
    SUPER_ADMIN,
    ADMIN,
    TEACHER,
    STUDENT;

    /** Attendance or system admin (includes Super Admin). */
    public boolean isAdminLevel() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }
}
