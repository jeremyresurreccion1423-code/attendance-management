package com.attendance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class ProfilePhotoService {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    private final Path uploadsDir;

    public ProfilePhotoService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadsDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public Path getUploadsDir() {
        return uploadsDir;
    }

    public String resolveProfilePhotoUrl(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        String safeName = username.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!Files.isDirectory(uploadsDir)) {
            return null;
        }

        for (String extension : SUPPORTED_EXTENSIONS) {
            Path candidate = uploadsDir.resolve(safeName + extension);
            if (Files.isRegularFile(candidate)) {
                try {
                    long version = Files.getLastModifiedTime(candidate).toMillis();
                    return "/uploads/" + candidate.getFileName() + "?v=" + version;
                } catch (IOException ignored) {
                    return "/uploads/" + candidate.getFileName();
                }
            }
        }

        return null;
    }

    public String saveProfilePhoto(String username, MultipartFile file) throws IOException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("User is required.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a profile image.");
        }

        Files.createDirectories(uploadsDir);

        String safeName = username.replaceAll("[^a-zA-Z0-9._-]", "_");
        String extension = normalizeExtension(file.getOriginalFilename());

        // Remove previous photo variants so only one file remains.
        for (String ext : SUPPORTED_EXTENSIONS) {
            Files.deleteIfExists(uploadsDir.resolve(safeName + ext));
        }

        Path target = uploadsDir.resolve(safeName + extension).normalize();
        if (!target.startsWith(uploadsDir)) {
            throw new IOException("Invalid upload path.");
        }

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + target.getFileName();
    }

    public String getInitial(String username) {
        if (username == null || username.isBlank()) {
            return "U";
        }
        return username.substring(0, 1).toUpperCase();
    }

    private static String normalizeExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".png";
        }
        String ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        return switch (ext) {
            case ".jpeg" -> ".jpg";
            case ".jpg", ".png", ".webp", ".gif", ".bmp" -> ext;
            default -> ".png";
        };
    }
}
