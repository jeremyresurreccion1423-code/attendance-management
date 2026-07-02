package com.attendance.service;

import com.attendance.model.*;
import com.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final TimetableRepository timetableRepository;

    @Value("${attendance.geo.radius:100}")
    private double geoRadius;

    @Value("${attendance.qr.grace-hours:1}")
    private int qrGraceHours;

    public List<Attendance> findBySubjectAndDate(Long subjectId, LocalDate date) {
        return attendanceRepository.findBySubjectIdAndAttendanceDate(subjectId, date);
    }

    public List<Attendance> findByStudent(Long studentId) {
        return attendanceRepository.findByStudentId(studentId);
    }

    @Transactional
    public Attendance recordManual(Long studentId, Long subjectId, AttendanceStatus status,
                                   String username, String remarks) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        validateEnrollment(studentId, subjectId);

        LocalDate today = LocalDate.now();
        Optional<Attendance> existing = attendanceRepository
                .findByStudentIdAndSubjectIdAndAttendanceDate(studentId, subjectId, today);

        Attendance attendance = existing.orElse(Attendance.builder()
                .student(student)
                .subject(subject)
                .attendanceDate(today)
                .build());

        attendance.setStatus(status);
        attendance.setMethod(AttendanceMethod.MANUAL);
        attendance.setTimeIn(LocalTime.now());
        attendance.setRemarks(remarks);
        userRepository.findByUsername(username).ifPresent(attendance::setRecordedBy);

        Attendance saved = attendanceRepository.save(attendance);
        notifyStudent(student, "Attendance Recorded", "Your attendance for " + subject.getSubjectName() + " was marked as " + status);
        checkLowAttendance(student, subject.getSubjectName());
        return saved;
    }

    @Transactional
    public Attendance recordQR(Long studentId, String qrCode, Double latitude, Double longitude) {
        if (qrCode == null || qrCode.trim().isEmpty()) {
            throw new IllegalArgumentException("QR code is required.");
        }
        String normalizedQrCode = qrCode.trim();
        AttendanceQR qr = qrService.findActiveByCode(normalizedQrCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired QR code"));

        if (qr.getLatitude() != null && qr.getLongitude() != null) {
            validateLocation(latitude, longitude, qr.getLatitude(), qr.getLongitude());
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        Subject subject = qr.getSubject();
        validateEnrollment(studentId, subject.getId());

        LocalDate today = LocalDate.now();
        if (attendanceRepository.findByStudentIdAndSubjectIdAndAttendanceDate(studentId, subject.getId(), today).isPresent()) {
            throw new IllegalArgumentException("Attendance already recorded for today");
        }

        AttendanceStatus status = resolveQrStatus(qr, today);

        Attendance attendance = Attendance.builder()
                .student(student)
                .subject(subject)
                .attendanceDate(today)
                .timeIn(LocalTime.now())
                .status(status)
                .method(AttendanceMethod.QR)
                .latitude(latitude)
                .longitude(longitude)
                .build();

        Attendance saved = attendanceRepository.save(attendance);
        notifyStudent(student, "Attendance Recorded", "Your QR attendance for " + subject.getSubjectName() + " was marked as " + status);
        checkLowAttendance(student, subject.getSubjectName());
        return saved;
    }

    public double getAttendancePercentage(Long studentId) {
        long total = attendanceRepository.countByStudentId(studentId);
        if (total == 0) return 0;
        long present = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.PRESENT);
        long late = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.LATE);
        return ((present + late) * 100.0) / total;
    }

    public List<Attendance> findByDateRange(LocalDate start, LocalDate end) {
        return attendanceRepository.findByAttendanceDateBetween(start, end);
    }

    @Transactional
    public Attendance markAutoAbsent(Long studentId, Long subjectId, LocalDate date, String remarks) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        if (attendanceRepository.findByStudentIdAndSubjectIdAndAttendanceDate(studentId, subjectId, date).isPresent()) {
            return attendanceRepository.findByStudentIdAndSubjectIdAndAttendanceDate(studentId, subjectId, date).get();
        }

        Attendance attendance = Attendance.builder()
                .student(student)
                .subject(subject)
                .attendanceDate(date)
                .status(AttendanceStatus.ABSENT)
                .method(AttendanceMethod.AUTO)
                .remarks(remarks)
                .build();

        Attendance saved = attendanceRepository.save(attendance);
        notifyStudent(student, "Marked Absent",
                "You were marked absent for " + subject.getSubjectName() + " — no QR attendance within "
                        + qrGraceHours + " hour(s) after class schedule.");
        checkLowAttendance(student, subject.getSubjectName());
        return saved;
    }

    private AttendanceStatus resolveQrStatus(AttendanceQR qr, LocalDate date) {
        LocalTime scheduleStart = resolveScheduleStartTime(qr, date);
        if (scheduleStart == null) {
            return AttendanceStatus.PRESENT;
        }
        LocalTime lateThreshold = scheduleStart.plusMinutes(15);
        return LocalTime.now().isAfter(lateThreshold) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
    }

    private LocalTime resolveScheduleStartTime(AttendanceQR qr, LocalDate date) {
        if (qr.getTimetable() != null && qr.getTimetable().getStartTime() != null) {
            return qr.getTimetable().getStartTime();
        }
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(date.getDayOfWeek().name());
        return timetableRepository
                .findBySubjectIdAndDayOfWeekAndPublishedTrue(qr.getSubject().getId(), dayOfWeek)
                .stream()
                .map(Timetable::getStartTime)
                .min(java.util.Comparator.naturalOrder())
                .orElse(null);
    }

    private void validateEnrollment(Long studentId, Long subjectId) {
        if (!enrollmentRepository.existsByStudentIdAndSubjectId(studentId, subjectId)) {
            throw new IllegalArgumentException("Student is not enrolled in this subject");
        }
    }

    private void validateLocation(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null) {
            throw new IllegalArgumentException("Location is required for QR attendance");
        }
        double distance = haversine(lat1, lon1, lat2, lon2);
        if (distance > geoRadius) {
            throw new IllegalArgumentException("You are too far from the class location (" + (int) distance + "m away)");
        }
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private void notifyStudent(Student student, String title, String message) {
        if (student.getUser() != null) {
            notificationRepository.save(Notification.builder()
                    .user(student.getUser())
                    .title(title)
                    .message(message)
                    .build());
        }
    }

    private void checkLowAttendance(Student student, String subjectName) {
        if (student.getUser() == null) {
            return;
        }
        double pct = getAttendancePercentage(student.getId());
        if (pct >= 75 || pct <= 0) {
            return;
        }
        if (!notificationRepository.existsByUserIdAndTitleAndCreatedAtAfter(
                student.getUser().getId(), "Low Attendance Alert", java.time.LocalDateTime.now().minusDays(7))) {
            notifyStudent(student, "Low Attendance Alert",
                    String.format("Your attendance%s is %.1f%%. Please improve to meet the 75%% requirement.",
                            subjectName != null ? " in " + subjectName : "", pct));
        }
    }

    private final QRService qrService;
}
