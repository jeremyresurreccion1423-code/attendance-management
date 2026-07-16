package com.attendance.controller;

import com.attendance.config.LibraryAppLinks;
import com.attendance.model.User;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.TeacherRepository;
import com.attendance.service.AttendanceMailService;
import com.attendance.service.AuthService;
import com.attendance.service.ProfilePhotoService;
import com.attendance.service.SharedAttendanceStudentProfileBridgeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.attendance.util.ValidationHelper;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private final AuthService authService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SharedAttendanceStudentProfileBridgeService sharedAttendanceStudentProfileBridgeService;
    private final AttendanceMailService attendanceMailService;
    private final ProfilePhotoService profilePhotoService;
    private final LibraryAppLinks libraryAppLinks;

    private final Map<String, OtpEntry> forgotPasswordOtpStore = new ConcurrentHashMap<>();

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String message,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String superAdmin,
                        Model model) {
        if ("session".equals(error)) {
            model.addAttribute("error", "Your session has expired. Please log in again.");
        } else if ("locked".equals(error)) {
            model.addAttribute("error", "Account locked after too many failed attempts. Try again in 15 minutes or contact an administrator.");
        } else if ("inactive".equals(error)) {
            model.addAttribute("error", message != null && !message.isBlank()
                    ? message
                    : "Your account is INACTIVE. You cannot log in at this time. Please contact the administrator.");
        } else if ("disabled".equals(error)) {
            model.addAttribute("error", "Your account has been disabled. Please contact the administrator.");
        } else if (error != null) {
            model.addAttribute("error", "Invalid username or password.");
        }
        if (superAdmin != null) {
            model.addAttribute("error", "Super Admin accounts must sign in via the Library System Control Center.");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
        }
        model.addAttribute("librarySuperAdminLoginUrl", libraryAppLinks.superAdminLogin());
        return "auth/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm(Authentication authentication, Model model) {
        boolean loggedIn = isLoggedIn(authentication);
        model.addAttribute("loggedInReset", loggedIn);

        if (loggedIn) {
            User user = authService.findByUsername(authentication.getName()).orElse(null);
            if (user != null) {
                if (!model.containsAttribute("emailOrUsername")) {
                    model.addAttribute("emailOrUsername", user.getUsername());
                }
                String email = resolveEmailForUser(user);
                model.addAttribute("profileEmail", email);
                model.addAttribute("maskedEmail", maskEmail(email));
            }
            model.addAttribute("otpLocked", true);
            if (!model.containsAttribute("backUrl")) {
                model.addAttribute("backUrl", "/profile");
                model.addAttribute("backLabel", "Back to Profile");
            }
        } else {
            if (!model.containsAttribute("otpLocked")) {
                model.addAttribute("otpLocked", false);
            }
            if (!model.containsAttribute("backUrl")) {
                model.addAttribute("backUrl", "/login");
                model.addAttribute("backLabel", "Back to Login");
            }
        }
        return "auth/forgot-password";
    }

    /**
     * Library-style forgot password:
     * 1) POST without otp → generate + email OTP
     * 2) POST with otp + newPassword → verify OTP then reset
     * Logged-in users do not enter email — OTP goes to their profile email automatically.
     */
    @PostMapping({"/forgot-password", "/forgot-password/request-otp"})
    public String forgotPasswordSubmit(@RequestParam(required = false) String emailOrUsername,
                                       @RequestParam(required = false) String username,
                                       @RequestParam(required = false) String currentPassword,
                                       @RequestParam(required = false) String otp,
                                       @RequestParam(required = false) String newPassword,
                                       @RequestParam(required = false) String confirmPassword,
                                       Authentication authentication,
                                       RedirectAttributes redirect) {
        String identifier = firstNonBlank(emailOrUsername, username);
        if ((identifier == null || identifier.isBlank()) && isLoggedIn(authentication)) {
            identifier = authentication.getName();
        }

        // Step 2: verify OTP and reset password
        if (otp != null && !otp.isBlank()) {
            return resetPasswordWithOtp(identifier, currentPassword, otp, newPassword, confirmPassword, authentication, redirect);
        }

        // Step 1: send OTP
        return requestPasswordResetOtp(identifier, authentication, redirect);
    }

    private String requestPasswordResetOtp(String identifier,
                                           Authentication authentication,
                                           RedirectAttributes redirect) {
        if (identifier == null || identifier.isBlank()) {
            redirect.addFlashAttribute("error",
                    isLoggedIn(authentication)
                            ? "Unable to resolve your account. Please log in again."
                            : "Enter your username or email.");
            return "redirect:/forgot-password";
        }

        Optional<User> userOpt = authService.findByUsernameOrEmail(identifier);
        if (userOpt.isEmpty()) {
            redirect.addFlashAttribute("error", "No account found with that username or email.");
            redirect.addFlashAttribute("emailOrUsername", identifier);
            return "redirect:/forgot-password";
        }

        User user = userOpt.get();
        // When logged in, only allow resetting your own account.
        if (isLoggedIn(authentication) && !authentication.getName().equalsIgnoreCase(user.getUsername())) {
            redirect.addFlashAttribute("error", "You can only reset the password for your own account.");
            return "redirect:/forgot-password";
        }

        String recipientEmail = resolveEmailForUser(user);
        if (recipientEmail == null || recipientEmail.isBlank()) {
            redirect.addFlashAttribute("error", "No email found on your profile. Update your profile email or contact admin.");
            redirect.addFlashAttribute("emailOrUsername", user.getUsername());
            return "redirect:/forgot-password";
        }

        if (!attendanceMailService.isConfigured()) {
            redirect.addFlashAttribute("error",
                    "Attendance mail is not configured. Set MAIL_PASSWORD (or BREVO_SMTP_PASSWORD) for Brevo SMTP.");
            redirect.addFlashAttribute("emailOrUsername", user.getUsername());
            return "redirect:/forgot-password";
        }

        String otp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
        forgotPasswordOtpStore.put(user.getUsername(), new OtpEntry(otp, LocalDateTime.now().plusMinutes(10)));

        try {
            attendanceMailService.sendText(
                    recipientEmail,
                    "Attendance Management System - Password Reset OTP",
                    "Your Attendance Management System OTP is " + otp
                            + ".\n\nIt expires in 10 minutes.\n\nIf you did not request this, ignore this email."
            );
            redirect.addFlashAttribute("otpRequired", true);
            redirect.addFlashAttribute("emailOrUsername", user.getUsername());
            redirect.addFlashAttribute("maskedEmail", maskEmail(recipientEmail));
            redirect.addFlashAttribute("message",
                    isLoggedIn(authentication)
                            ? "OTP sent to your registered email (" + maskEmail(recipientEmail) + "). Enter the code to reset your password."
                            : "OTP sent to your email: " + recipientEmail + ". Enter the code to reset your password.");
        } catch (Exception ex) {
            forgotPasswordOtpStore.remove(user.getUsername());
            log.error("Failed to send Attendance OTP email to {}: {}", recipientEmail, ex.getMessage(), ex);
            redirect.addFlashAttribute("error", "Unable to send OTP email (" + rootMessage(ex) + ")");
            redirect.addFlashAttribute("emailOrUsername", user.getUsername());
        }
        return "redirect:/forgot-password";
    }

    private String resetPasswordWithOtp(String identifier,
                                        String currentPassword,
                                        String otp,
                                        String newPassword,
                                        String confirmPassword,
                                        Authentication authentication,
                                        RedirectAttributes redirect) {
        if (identifier == null || identifier.isBlank()) {
            redirect.addFlashAttribute("error", "Account identifier is missing. Request a new OTP.");
            return "redirect:/forgot-password";
        }

        Optional<User> userOpt = authService.findByUsernameOrEmail(identifier);
        String storeKey = userOpt.map(User::getUsername).orElse(identifier.trim());
        OtpEntry entry = forgotPasswordOtpStore.get(storeKey);

        if (entry == null || entry.expiresAt().isBefore(LocalDateTime.now()) || !entry.code().equals(otp.trim())) {
            redirect.addFlashAttribute("error", "Invalid or expired OTP.");
            redirect.addFlashAttribute("otpRequired", true);
            redirect.addFlashAttribute("emailOrUsername", storeKey);
            return "redirect:/forgot-password";
        }

        if (isLoggedIn(authentication)) {
            User user = userOpt.orElse(null);
            if (user == null || currentPassword == null || currentPassword.isBlank()
                    || !authService.verifyPassword(user, currentPassword)) {
                redirect.addFlashAttribute("error", "Current password is incorrect.");
                redirect.addFlashAttribute("otpRequired", true);
                redirect.addFlashAttribute("emailOrUsername", storeKey);
                return "redirect:/forgot-password";
            }
        }

        if (newPassword == null || confirmPassword == null || !newPassword.equals(confirmPassword)) {
            redirect.addFlashAttribute("error", "New password and confirmation do not match.");
            redirect.addFlashAttribute("otpRequired", true);
            redirect.addFlashAttribute("emailOrUsername", storeKey);
            return "redirect:/forgot-password";
        }
        try {
            ValidationHelper.validatePassword(newPassword);
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            redirect.addFlashAttribute("otpRequired", true);
            redirect.addFlashAttribute("emailOrUsername", storeKey);
            return "redirect:/forgot-password";
        }

        if (authService.resetPassword(storeKey, newPassword)) {
            forgotPasswordOtpStore.remove(storeKey);
            redirect.addFlashAttribute("message", "Password reset successful! Please log in with your new password.");
            return "redirect:/login";
        }

        redirect.addFlashAttribute("error", "Account not found.");
        return "redirect:/forgot-password";
    }

    private static boolean isLoggedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equalsIgnoreCase(authentication.getName());
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "your registered email";
        }
        String[] parts = email.trim().split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        String maskedLocal = local.length() <= 2
                ? local.charAt(0) + "***"
                : local.substring(0, 2) + "***";
        return maskedLocal + "@" + domain;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
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
            case SUPER_ADMIN -> "/admin/dashboard";
            case ADMIN -> "/admin/dashboard";
            case TEACHER -> "/teacher/dashboard";
            case STUDENT -> "/student/dashboard";
        });
        return "profile/dashboard";
    }

    @PostMapping("/profile/password")
    public String changePassword(RedirectAttributes redirect) {
        // Password changes (forgot or change) always go through the same OTP flow.
        redirect.addFlashAttribute("message", "Password changes require OTP verification via email.");
        return "redirect:/forgot-password";
    }

    @PostMapping("/profile/photo")
    public String uploadPhoto(@RequestParam("photo") MultipartFile file,
                              Authentication auth,
                              RedirectAttributes redirect,
                              HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            redirect.addFlashAttribute("error", "Please choose a profile image.");
            return redirectToReferrerOrDashboard(request);
        }

        try {
            ValidationHelper.validateProfilePhoto(file);
            User user = authService.findByUsername(auth.getName())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + auth.getName()));
            profilePhotoService.saveProfilePhoto(user.getUsername(), file);
            redirect.addFlashAttribute("message", "Profile photo updated successfully.");
        } catch (IOException e) {
            log.error("Unable to upload profile photo for {}: {}", auth.getName(), e.getMessage(), e);
            redirect.addFlashAttribute("error",
                    "Unable to upload profile photo. Please try again with a JPG or PNG under 5MB.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected profile photo upload failure for {}: {}", auth.getName(), ex.getMessage(), ex);
            redirect.addFlashAttribute("error", "Unable to upload profile photo.");
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

    private static String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String msg = current.getMessage();
        if (!StringUtils.hasText(msg)) {
            msg = current.getClass().getSimpleName();
        }
        // Prefer the outer actionable message when we wrapped connectivity failures.
        if (ex.getMessage() != null && ex.getMessage().contains("Railway")) {
            msg = ex.getMessage();
        }
        return msg.length() > 220 ? msg.substring(0, 217) + "..." : msg;
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

        if (user.getRole() == com.attendance.model.Role.SUPER_ADMIN) {
            return "redirect:/admin/dashboard";
        } else if (user.getRole() == com.attendance.model.Role.ADMIN) {
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
