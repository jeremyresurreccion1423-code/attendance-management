package com.attendance.controller;

import com.attendance.exception.BusinessException;
import com.attendance.model.*;
import com.attendance.service.*;
import com.attendance.util.ValidationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final DashboardService dashboardService;
    private final TeacherService teacherService;
    private final SubjectService subjectService;
    private final AttendanceService attendanceService;
    private final MarkService markService;
    private final TimetableService timetableService;
    private final QRService qrService;
    private final ReportService reportService;
    private final AuthService authService;
    private final ProfilePhotoService profilePhotoService;
    private final AttendanceAutoAbsentService attendanceAutoAbsentService;

    private Teacher getCurrentTeacher(Authentication auth) {
        User user = authService.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found: " + auth.getName()));
        return teacherService.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Teacher record not found for user: " + auth.getName()));
    }

    private Subject requireOwnedSubject(Teacher teacher, Long subjectId) {
        Subject subject = subjectService.findById(subjectId)
                .orElseThrow(() -> new BusinessException("Subject not found."));
        if (subject.getTeacher() == null || !subject.getTeacher().getId().equals(teacher.getId())) {
            throw new BusinessException("You can only access subjects assigned to you.");
        }
        return subject;
    }

    @ExceptionHandler(BusinessException.class)
    public String handleTeacherBusinessException(BusinessException ex, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", ex.getMessage());
        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        Teacher teacher = getCurrentTeacher(auth);
        model.addAttribute("data", dashboardService.getTeacherDashboard(teacher.getId()));
        model.addAttribute("teacher", teacher);
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(auth.getName()));
        return "teacher/dashboard";
    }

    @GetMapping("/trends")
    public String trends(Authentication auth, Model model) {
        Teacher teacher = getCurrentTeacher(auth);
        model.addAttribute("data", dashboardService.getTeacherTrendsData(teacher.getId()));
        model.addAttribute("teacher", teacher);
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(auth.getName()));
        return "teacher/trends";
    }

    @GetMapping("/subjects")
    public String subjects(Authentication auth, Model model,
                           @RequestParam(required = false) Long subjectId) {
        Teacher teacher = getCurrentTeacher(auth);
        List<Subject> subjects = subjectService.findByTeacherId(teacher.getId());
        model.addAttribute("subjects", subjects);
        model.addAttribute("teacher", teacher);
        model.addAttribute("profilePhotoUrl", profilePhotoService.resolveProfilePhotoUrl(auth.getName()));

        Map<Long, Integer> enrollmentCounts = new HashMap<>();
        for (Subject subject : subjects) {
            enrollmentCounts.put(subject.getId(), subjectService.getEnrollments(subject.getId()).size());
        }
        model.addAttribute("enrollmentCounts", enrollmentCounts);

        if (subjectId != null) {
            Subject selected = requireOwnedSubject(teacher, subjectId);
            model.addAttribute("selectedSubject", selected);
            model.addAttribute("enrollments", subjectService.getEnrollments(subjectId));
        }
        return "teacher/subjects";
    }

    @GetMapping("/attendance")
    public String attendance(Authentication auth, Model model,
                             @RequestParam(required = false) Long subjectId,
                             @RequestParam(required = false) Long timetableId,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Teacher teacher = getCurrentTeacher(auth);
        List<Subject> subjects = subjectService.findByTeacherId(teacher.getId());
        model.addAttribute("subjects", subjects);
        LocalDate selectedDate = date != null ? date : LocalDate.now();
        model.addAttribute("selectedDate", selectedDate);
        if (subjectId != null) {
            requireOwnedSubject(teacher, subjectId);
            List<Timetable> availableSchedules = timetableService.findPublishedBySubjectAndDate(subjectId, selectedDate);
            model.addAttribute("availableSchedules", availableSchedules);
            Long selectedTimetableId = timetableId;
            if (selectedTimetableId == null && !availableSchedules.isEmpty()) {
                selectedTimetableId = availableSchedules.get(0).getId();
            }
            model.addAttribute("selectedTimetableId", selectedTimetableId);
            if (selectedDate.equals(LocalDate.now())) {
                int autoMarked = attendanceAutoAbsentService.processForSubject(subjectId, selectedDate);
                if (autoMarked > 0) {
                    model.addAttribute("message", autoMarked + " student(s) auto-marked absent (no QR scan within 1 hour after schedule).");
                }
            }
            model.addAttribute("selectedSubject", subjectId);
            model.addAttribute("attendanceRecords", attendanceService.findBySubjectAndDate(subjectId, selectedDate));
            model.addAttribute("enrollments", subjectService.getEnrollments(subjectId));
        }
        model.addAttribute("statuses", AttendanceStatus.values());
        return "teacher/attendance";
    }

    @PostMapping("/attendance/manual")
    public String manualAttendance(@RequestParam Long studentId,
                                   @RequestParam Long subjectId,
                                   @RequestParam AttendanceStatus status,
                                   @RequestParam(required = false) String remarks,
                                   Authentication auth,
                                   RedirectAttributes redirect) {
        try {
            requireOwnedSubject(getCurrentTeacher(auth), subjectId);
            attendanceService.recordManual(studentId, subjectId, status, auth.getName(), remarks);
            redirect.addFlashAttribute("message", "Attendance recorded");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teacher/attendance?subjectId=" + subjectId;
    }

    @GetMapping("/attendance/qr")
    public String qrAttendance(@RequestParam Long subjectId,
                               @RequestParam(required = false) Long timetableId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(required = false) Double latitude,
                               @RequestParam(required = false) Double longitude,
                               Authentication auth,
                               Model model,
                               RedirectAttributes redirect) {
        try {
            requireOwnedSubject(getCurrentTeacher(auth), subjectId);
            AttendanceQR qr = qrService.generateQR(subjectId, date, timetableId, latitude, longitude);
            LocalDate sessionDate = qr.getSessionDate() != null ? qr.getSessionDate() : LocalDate.now();
            List<Attendance> records = attendanceService.findBySubjectAndDate(subjectId, sessionDate);
            long presentCount = records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
            long lateCount = records.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count();
            long absentCount = records.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
            long scannedCount = presentCount + lateCount;
            int totalStudents = subjectService.getEnrollments(subjectId).size();

            model.addAttribute("qr", qr);
            model.addAttribute("qrImage", qrService.generateQRImageBase64(qr.getQrCode()));
            model.addAttribute("latitude", latitude);
            model.addAttribute("longitude", longitude);
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("presentCount", presentCount);
            model.addAttribute("lateCount", lateCount);
            model.addAttribute("absentCount", absentCount);
            model.addAttribute("scannedCount", scannedCount);
            model.addAttribute("sessionActive", Boolean.TRUE.equals(qr.getActive())
                    && qr.getExpiresAt() != null
                    && qr.getExpiresAt().isAfter(java.time.LocalDateTime.now()));
            return "teacher/qr-display";
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            StringBuilder redirectUrl = new StringBuilder("redirect:/teacher/attendance?subjectId=").append(subjectId);
            if (date != null) {
                redirectUrl.append("&date=").append(date);
            }
            if (timetableId != null) {
                redirectUrl.append("&timetableId=").append(timetableId);
            }
            return redirectUrl.toString();
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", "Failed to generate QR code. Please try again.");
            StringBuilder redirectUrl = new StringBuilder("redirect:/teacher/attendance?subjectId=").append(subjectId);
            if (date != null) {
                redirectUrl.append("&date=").append(date);
            }
            return redirectUrl.toString();
        }
    }

    @GetMapping("/marks")
    public String marks(Authentication auth, Model model, @RequestParam(required = false) Long subjectId) {
        Teacher teacher = getCurrentTeacher(auth);
        model.addAttribute("subjects", subjectService.findByTeacherId(teacher.getId()));
        if (subjectId != null) {
            requireOwnedSubject(teacher, subjectId);
            model.addAttribute("selectedSubject", subjectId);
            model.addAttribute("marks", markService.findBySubject(subjectId));
            model.addAttribute("enrollments", subjectService.getEnrollments(subjectId));
        }
        return "teacher/marks";
    }

    @PostMapping("/marks")
    public String saveMarks(@RequestParam Long studentId,
                            @RequestParam Long subjectId,
                            @RequestParam(required = false) Double quizScore,
                            @RequestParam(required = false) Double examScore,
                            @RequestParam(required = false) Double assignmentScore,
                            Authentication auth,
                            RedirectAttributes redirect) {
        try {
            requireOwnedSubject(getCurrentTeacher(auth), subjectId);
            ValidationHelper.validateMarkScore(quizScore, "Quiz score");
            ValidationHelper.validateMarkScore(examScore, "Exam score");
            ValidationHelper.validateMarkScore(assignmentScore, "Assignment score");
            markService.saveOrUpdate(studentId, subjectId, quizScore, examScore, assignmentScore);
            redirect.addFlashAttribute("message", "Marks saved");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/marks?subjectId=" + subjectId;
    }

    @PostMapping("/marks/compute")
    public String computeGrades(@RequestParam Long subjectId,
                                Authentication auth,
                                RedirectAttributes redirect) {
        requireOwnedSubject(getCurrentTeacher(auth), subjectId);
        markService.computeAllGradesForSubject(subjectId);
        redirect.addFlashAttribute("message", "Final grades computed");
        return "redirect:/teacher/marks?subjectId=" + subjectId;
    }

    @GetMapping("/timetable")
    public String timetable(Authentication auth, Model model) {
        Teacher teacher = getCurrentTeacher(auth);
        model.addAttribute("schedules", timetableService.findByTeacher(teacher.getId()));
        model.addAttribute("subjects", subjectService.findByTeacherId(teacher.getId()));
        Timetable schedule = new Timetable();
        schedule.setSubject(new Subject());
        model.addAttribute("schedule", schedule);
        model.addAttribute("days", DayOfWeek.values());
        return "teacher/timetable";
    }

    @PostMapping("/timetable")
    public String createSchedule(Authentication auth,
                                 @ModelAttribute Timetable timetable,
                                 RedirectAttributes redirect) {
        Teacher teacher = getCurrentTeacher(auth);
        if (timetable.getSubject() == null || timetable.getSubject().getId() == null) {
            redirect.addFlashAttribute("error", "Subject is required.");
            return "redirect:/teacher/timetable";
        }
        requireOwnedSubject(teacher, timetable.getSubject().getId());
        timetable.setTeacher(teacher);
        timetableService.save(timetable);
        redirect.addFlashAttribute("message", "Schedule created");
        return "redirect:/teacher/timetable";
    }

    @PostMapping("/timetable/{id}/publish")
    public String publishSchedule(@PathVariable Long id, RedirectAttributes redirect) {
        timetableService.publish(id);
        redirect.addFlashAttribute("message", "Schedule published");
        return "redirect:/teacher/timetable";
    }

    @PostMapping("/timetable/{id}/update")
    public String updateSchedule(@PathVariable Long id,
                                 @ModelAttribute Timetable timetable,
                                 RedirectAttributes redirect) {
        try {
            timetableService.update(id, timetable);
            redirect.addFlashAttribute("message", "Schedule updated");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teacher/timetable";
    }

    @PostMapping("/timetable/{id}/delete")
    public String deleteSchedule(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            timetableService.delete(id);
            redirect.addFlashAttribute("message", "Schedule deleted");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teacher/timetable";
    }

    @GetMapping("/reports")
    public String reports(Authentication auth, Model model) {
        Teacher teacher = getCurrentTeacher(auth);
        model.addAttribute("subjects", subjectService.findByTeacherId(teacher.getId()));
        return "teacher/reports";
    }

    @GetMapping("/reports/attendance/excel")
    public Object exportAttendanceExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            RedirectAttributes redirect) throws Exception {
        String validationRedirect = validateReportDateRange(start, end, redirect);
        if (validationRedirect != null) {
            return validationRedirect;
        }
        byte[] data = reportService.exportAttendanceExcel(start, end);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance_report.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/reports/attendance/pdf")
    public Object exportAttendancePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            RedirectAttributes redirect) throws Exception {
        String validationRedirect = validateReportDateRange(start, end, redirect);
        if (validationRedirect != null) {
            return validationRedirect;
        }
        byte[] data = reportService.exportAttendancePdf(start, end);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    private String validateReportDateRange(LocalDate start, LocalDate end, RedirectAttributes redirect) {
        if (start == null || end == null) {
            redirect.addFlashAttribute("error", "Please select both start and end dates.");
            return "redirect:/teacher/reports";
        }
        if (end.isBefore(start)) {
            redirect.addFlashAttribute("error", "End date must be on or after start date.");
            return "redirect:/teacher/reports";
        }
        return null;
    }

    @GetMapping("/reports/grades/excel")
    public ResponseEntity<byte[]> exportGradesExcel(@RequestParam Long subjectId,
                                                    Authentication auth) throws Exception {
        requireOwnedSubject(getCurrentTeacher(auth), subjectId);
        byte[] data = reportService.exportGradesExcel(subjectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=grade_report.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
