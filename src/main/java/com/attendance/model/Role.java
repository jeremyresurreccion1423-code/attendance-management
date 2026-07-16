package com.attendance.model;

public enum Role {
    ADMIN,
    TEACHER,
    STUDENT;

    /** Attendance or system admin. */
    public boolean isAdminLevel() {
        return this == ADMIN;
    }
}
