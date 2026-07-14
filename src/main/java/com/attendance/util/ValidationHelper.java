package com.attendance.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ValidationHelper {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
    private static final long MAX_PHOTO_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_PHOTO_EXT = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private ValidationHelper() {}

    public static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }

    public static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (!EMAIL.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
    }

    /** Strong password: min 8 chars, upper, lower, number, special character. */
    public static void validatePassword(String password) {
        if (password == null || !STRONG_PASSWORD.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and include uppercase, lowercase, number, and special character.");
        }
    }

    public static void validatePasswordsMatch(String password, String confirm) {
        if (password == null || !password.equals(confirm)) {
            throw new IllegalArgumentException("Password and confirmation do not match.");
        }
    }

    public static void validateMarkScore(Double score, String fieldName) {
        if (score == null) {
            return;
        }
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 100.");
        }
    }

    public static void validateProfilePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a profile image.");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new IllegalArgumentException("Profile image must be 5MB or smaller.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_PHOTO_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Profile image must be JPG, PNG, or WEBP only.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot) : "";
        if (!ALLOWED_PHOTO_EXT.contains(ext)) {
            throw new IllegalArgumentException("Profile image extension must be .jpg, .jpeg, .png, or .webp.");
        }
        if (name.endsWith(".exe") || name.endsWith(".php") || name.endsWith(".jsp") || name.endsWith(".bat")
                || name.endsWith(".sh") || name.endsWith(".js")) {
            throw new IllegalArgumentException("This file type is not allowed.");
        }
    }
}
