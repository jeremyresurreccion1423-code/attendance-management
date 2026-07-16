package com.attendance.service;

import com.attendance.model.AttendanceQR;
import com.attendance.model.DayOfWeek;
import com.attendance.model.Subject;
import com.attendance.model.Timetable;
import com.attendance.repository.AttendanceQRRepository;
import com.attendance.repository.SubjectRepository;
import com.attendance.repository.TimetableRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QRService {

    private final AttendanceQRRepository qrRepository;
    private final SubjectRepository subjectRepository;
    private final TimetableRepository timetableRepository;

    @Transactional
    public AttendanceQR generateQR(Long subjectId, LocalDate sessionDate, Long timetableId, Double latitude, Double longitude) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        LocalDate targetDate = sessionDate != null ? sessionDate : LocalDate.now();

        // QR attendance window: 15 minutes from generation
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        Timetable selectedTimetable = null;
        if (timetableId != null) {
            Timetable timetable = timetableRepository.findById(timetableId)
                    .orElseThrow(() -> new IllegalArgumentException("Schedule not found."));
            selectedTimetable = timetable;
            if (timetable.getSubject() == null || !timetable.getSubject().getId().equals(subjectId)) {
                throw new IllegalArgumentException("Selected schedule does not belong to the selected subject.");
            }
            if (!Boolean.TRUE.equals(timetable.getPublished())) {
                throw new IllegalArgumentException("Only published schedules can be used for QR generation.");
            }
            DayOfWeek expectedDay = DayOfWeek.valueOf(targetDate.getDayOfWeek().name());
            if (timetable.getDayOfWeek() != expectedDay) {
                throw new IllegalArgumentException("Selected schedule does not match the chosen date.");
            }
        }

        String code = UUID.randomUUID().toString();
        AttendanceQR qr = AttendanceQR.builder()
                .subject(subject)
                .timetable(selectedTimetable)
                .qrCode(code)
                .sessionDate(targetDate)
                .expiresAt(expiresAt)
                .latitude(latitude)
                .longitude(longitude)
                .active(true)
                .build();

        return qrRepository.save(qr);
    }

    public Optional<AttendanceQR> findActiveByCode(String code) {
        return qrRepository.findByQrCodeAndActiveTrue(code)
                .filter(qr -> qr.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    public String generateQRImageBase64(String data) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(data, BarcodeFormat.QR_CODE, 320, 320);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR image", e);
        }
    }
}
