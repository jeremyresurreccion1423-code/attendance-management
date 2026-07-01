package com.attendance.service;

import com.attendance.model.*;
import com.attendance.repository.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final MarkRepository markRepository;

    public byte[] exportAttendanceExcel(LocalDate start, LocalDate end) throws Exception {
        List<Attendance> records = attendanceRepository.findByAttendanceDateBetween(start, end);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance Report");
            Row header = sheet.createRow(0);
            String[] columns = {"Date", "Student", "Subject", "Time In", "Status", "Method"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
            }
            int rowNum = 1;
            for (Attendance a : records) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(a.getAttendanceDate().toString());
                row.createCell(1).setCellValue(a.getStudent().getFullName());
                row.createCell(2).setCellValue(a.getSubject().getSubjectName());
                row.createCell(3).setCellValue(a.getTimeIn() != null ? a.getTimeIn().toString() : "");
                row.createCell(4).setCellValue(a.getStatus().name());
                row.createCell(5).setCellValue(a.getMethod().name());
            }
            for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportAttendancePdf(LocalDate start, LocalDate end) throws Exception {
        List<Attendance> records = attendanceRepository.findByAttendanceDateBetween(start, end);
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("Attendance Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
        document.add(new Paragraph("Period: " + start + " to " + end));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        addPdfHeader(table, "Date", "Student", "Subject", "Status", "Method");
        for (Attendance a : records) {
            table.addCell(a.getAttendanceDate().toString());
            table.addCell(a.getStudent().getFullName());
            table.addCell(a.getSubject().getSubjectName());
            table.addCell(a.getStatus().name());
            table.addCell(a.getMethod().name());
        }
        document.add(table);
        document.close();
        return out.toByteArray();
    }

    public byte[] exportGradesExcel(Long subjectId) throws Exception {
        List<Mark> marks = markRepository.findBySubjectId(subjectId);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Grade Report");
            Row header = sheet.createRow(0);
            String[] columns = {"Student", "Quiz", "Assignment", "Exam", "Final Grade"};
            for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);
            int rowNum = 1;
            for (Mark m : marks) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(m.getStudent().getFullName());
                row.createCell(1).setCellValue(m.getQuizScore());
                row.createCell(2).setCellValue(m.getAssignmentScore());
                row.createCell(3).setCellValue(m.getExamScore());
                row.createCell(4).setCellValue(m.getFinalGrade() != null ? m.getFinalGrade() : 0);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportStudentListPdf() throws Exception {
        List<Student> students = studentRepository.findAll();
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("Student Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
        document.add(Chunk.NEWLINE);
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        addPdfHeader(table, "Student No.", "Name", "Course", "Year", "Status");
        for (Student s : students) {
            table.addCell(s.getStudentNumber());
            table.addCell(s.getFullName());
            table.addCell(s.getDepartment() != null ? s.getDepartment().getName() : "");
            table.addCell(s.getYearLevel() != null ? s.getYearLevel() : "");
            table.addCell(s.getStatus().name());
        }
        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private void addPdfHeader(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            table.addCell(cell);
        }
    }
}
