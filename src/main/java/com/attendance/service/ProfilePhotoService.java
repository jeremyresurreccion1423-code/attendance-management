package com.attendance.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ProfilePhotoService {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    public String resolveProfilePhotoUrl(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        String safeName = username.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();

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

    public String getInitial(String username) {
        if (username == null || username.isBlank()) {
            return "U";
        }
        return username.substring(0, 1).toUpperCase();
    }
}
