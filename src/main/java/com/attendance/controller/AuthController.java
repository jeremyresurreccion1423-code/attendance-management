package com.attendance.controller;

import com.attendance.model.User;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.TeacherRepository;
import com.attendance.service.AuthService;
import com.attendance.service.SharedAttendanceStudentProfileBridgeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.attendance.util.ValidationHelper;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SharedAttendanceStudentProfileBridgeService sharedAttendanceStudentProfileBridgeService;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    private final Map<String, OtpEntry> forgotPasswordOtpStore = new ConcurrentHashMap<>();

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String message,
                        @RequestParam(required = false) String logout,
                        Model model) {
        if ("session".equals(error)) {
            model.addAttribute("error", "Your session has expired. Please log in again.");
        } else if ("inactive".equals(error)) {
            model.addAttribute("error", message != null && !message.isBlank()
                    ? message
                    : "Your student account is INACTIVE. You cannot log in at this time. Please contact the administrator.");
        } else if ("disabled".equals(error)) {
            model.addAttribute("error", "Your account has been disabled. Please contact the administrator.");
        } else if (error != null) {
            model.addAttribute("error", "Invalid username or password.");
        }
        if (logout != null) model.addAttribute("message", "You have been logged out successfully.");
        return "auth/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password/request-otp")
    public String requestForgotPasswordOtp(@RequestParam String username,
                                           RedirectAttributes redirect) {
        Optional<User> userOpt = authService.findByUsernameOrEmail(username);
        if (userOpt.isEmpty()) {
            redirect.addFlashAttribute("error", "Account not found.");
            return "redirect:/forgot-password";
        }

        User user = userOpt.get();
        String recipientEmail = resolveEmailForUser(user);
        if (recipientEmail == null || recipientEmail.isBlank()) {
            redirect.addFlashAttribute("error", "No email found for this account. Contact admin.");
            return "redirect:/forgot-password";
        }

        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        forgotPasswordOtpStore.put(user.getUsername(), new OtpEntry(otp, LocalDateTime.now().plusMinutes(10)));

        try {
            if (mailUsername == null || mailUsername.isBlank()) {
                redirect.addFlashAttribute("error", "Mail is not configured. Set spring.mail.username and password in application-local.properties.");
                return "redirect:/forgot-password";
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(recipientEmail);
            message.setSubject("AMS Password Reset OTP");
            message.setText("Your AMS OTP is " + otp + ". It will expire in 10 minutes.");
            mailSender.send(message);
            redirect.addFlashAttribute("message", "OTP sent to your Gmail: " + recipientEmail);
            redirect.addFlashAttribute("otpUsername", user.getUsername());
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", "Unable to send OTP email. Check mail configuration.");
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        User user = authService.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found: " + auth.getName()));
        Map<String, String> profileDetails = new LinkedHashMap<>();
        String displayName = user.getUsername();
        String code = "ID " + user.getId();

        if (user.getRole() == com.attendance.model.Role.STUDENT) {
            studentRepository.findByUserId(user.getId()).ifPresent(student -> {
                model.addAttribute("displayName", student.getFullName());
                model.addAttribute("profileCode", student.getStudentNumber());
                profileDetails.put("Name", student.getFullName());
                profileDetails.put("Department", student.getDepartment() != null ? student.getDepartment().getName() : "-");
                profileDetails.put("Year Level", student.getYearLevel() != null ? student.getYearLevel() : "-");
                profileDetails.put("Section", student.getSection() != null ? student.getSection().getName() : "-");
                profileDetails.put("Contact", student.getContactNumber() != null ? student.getContactNumber() : "-");
                profileDetails.put("Email", student.getEmail() != null ? student.getEmail() : "-");
            });
        } else if (user.getRole() == com.attendance.model.Role.TEACHER) {
            teacherRepository.findByUserId(user.getId()).ifPresent(teacher -> {
                model.addAttribute("displayName", teacher.getFullName());
                model.addAttribute("profileCode", teacher.getEmployeeId());
                profileDetails.put("Name", teacher.getFullName());
                profileDetails.put("Department", teacher.getDepartment() != null ? teacher.getDepartment().getName() : "-");
                profileDetails.put("Contact", teacher.getContactNumber() != null ? teacher.getContactNumber() : "-");
                profileDetails.put("Email", teacher.getEmail() != null ? teacher.getEmail() : "-");
            });
        }

        if (!model.containsAttribute("displayName")) {
            model.addAttribute("displayName", displayName);
        }
        if (!model.containsAttribute("profileCode")) {
            model.addAttribute("profileCode", code);
        }
        if (profileDetails.isEmpty()) {
            profileDetails.put("Username", user.getUsername());
            profileDetails.put("Role", user.getRole().name().toLowerCase());
            profileDetails.put("Account", Boolean.TRUE.equals(user.getEnabled()) ? "Active" : "Disabled");
            profileDetails.put("Created At", user.getCreatedAt() != null
                    ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                    : "-");
        }

        LocalDate today = LocalDate.now();
        model.addAttribute("user", user);
        model.addAttribute("roleName", user.getRole().name());
        model.addAttribute("todayDate", today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        model.addAttribute("todayDay", today.format(DateTimeFormatter.ofPattern("EEEE")));
        model.addAttribute("profileDetails", profileDetails);
        model.addAttribute("dashboardPath", switch (user.getRole()) {
            case ADMIN -> "/admin/dashboard";
            case TEACHER -> "/teacher/dashboard";
            case STUDENT -> "/student/dashboard";
        });
        return "profile/dashboard";
    }

    @PostMapping("/forgot-password")
    public String resetPassword(@RequestParam String username,
                                @RequestParam String otp,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                RedirectAttributes redirect) {
        OtpEntry entry = forgotPasswordOtpStore.get(username);
        if (entry == null || entry.expiresAt().isBefore(LocalDateTime.now()) || !entry.code().equals(otp)) {
            redirect.addFlashAttribute("error", "Invalid or expired OTP.");
            redirect.addFlashAttribute("otpUsername", username);
            return "redirect:/forgot-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirect.addFlashAttribute("error", "New password and confirmation do not match.");
            redirect.addFlashAttribute("otpUsername", username);
            return "redirect:/forgot-password";
        }
        try {
            ValidationHelper.validatePassword(newPassword);
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            redirect.addFlashAttribute("otpUsername", username);
            return "redirect:/forgot-password";
        }

        if (authService.resetPassword(username, newPassword)) {
            forgotPasswordOtpStore.remove(username);
            redirect.addFlashAttribute("message", "Password updated successfully. You can now login.");
        } else {
            redirect.addFlashAttribute("error", "Username not found");
        }
        return "redirect:/login";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth,
                                 RedirectAttributes redirect,
                                 HttpServletRequest request) {
        User user = authService.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found: " + auth.getName()));

        if (!newPassword.equals(confirmPassword)) {
            redirect.addFlashAttribute("error", "New password and confirmation do not match.");
            return redirectToReferrerOrDashboard(request);
        }
        try {
            ValidationHelper.validatePassword(newPassword);
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return redirectToReferrerOrDashboard(request);
        }
        if (!authService.verifyPassword(user, currentPassword)) {
            redirect.addFlashAttribute("error", "Current password is incorrect.");
            return redirectToReferrerOrDashboard(request);
        }

        authService.changePassword(user, newPassword);
        redirect.addFlashAttribute("message", "Password updated successfully.");
        return redirectToReferrerOrDashboard(request);
    }

    @PostMapping("/profile/photo")
    public String uploadPhoto(@RequestParam("photo") MultipartFile file,
                              Authentication auth,
                              RedirectAttributes redirect,
                              HttpServletRequest request) {
        if (file.isEmpty()) {
            redirect.addFlashAttribute("error", "Please choose a profile image.");
            return redirectToReferrerOrDashboard(request);
        }

        try {
            ValidationHelper.validateProfilePhoto(file);
            User user = authService.findByUsername(auth.getName())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + auth.getName()));
            String safeName = user.getUsername().replaceAll("[^a-zA-Z0-9._-]", "_");
            String extension = getExtension(file.getOriginalFilename());
            Path uploadDir = Paths.get("uploads");
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(safeName + extension);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            redirect.addFlashAttribute("message", "Profile photo updated successfully.");
        } catch (IOException e) {
            redirect.addFlashAttribute("error", "Unable to upload profile photo.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return redirectToReferrerOrDashboard(request);
    }

    private String redirectToReferrerOrDashboard(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/dashboard";
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".png";
        }
        return fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
    }

    private String resolveEmailForUser(User user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        if (user.getRole() == com.attendance.model.Role.STUDENT) {
            return studentRepository.findByUserId(user.getId()).map(s -> s.getEmail()).orElse(null);
        }
        if (user.getRole() == com.attendance.model.Role.TEACHER) {
            return teacherRepository.findByUserId(user.getId()).map(t -> t.getEmail()).orElse(null);
        }
        return user.getUsername() != null && user.getUsername().contains("@") ? user.getUsername() : null;
    }

    private record OtpEntry(String code, LocalDateTime expiresAt) {}

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, RedirectAttributes redirect) {
        User user = authService.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found: " + auth.getName()));
        authService.updateLastLogin(auth.getName());

        if (user.getRole() == com.attendance.model.Role.ADMIN) {
            return "redirect:/admin/dashboard";
        } else if (user.getRole() == com.attendance.model.Role.TEACHER) {
            if (teacherRepository.findByUserId(user.getId()).isEmpty()) {
                redirect.addFlashAttribute("error",
                        "Your account is valid, but no teacher profile exists in Attendance yet. Contact an administrator.");
                return "redirect:/login?logout=true";
            }
            return "redirect:/teacher/dashboard";
        } else if (user.getRole() == com.attendance.model.Role.STUDENT) {
            sharedAttendanceStudentProfileBridgeService.ensureAttendanceStudent(user);
            if (studentRepository.findByUserId(user.getId()).isEmpty()) {
                redirect.addFlashAttribute("error",
                        "Your account is valid, but no student profile exists in Attendance yet. Contact an administrator.");
                return "redirect:/login?logout=true";
            }
            return "redirect:/student/dashboard";
        }

        return "redirect:/login";
    }
}
