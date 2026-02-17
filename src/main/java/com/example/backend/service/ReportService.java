package com.example.backend.service;

import com.example.backend.domain.Student;
import com.example.backend.dto.ExamResult;
import com.example.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ReportService {

    private final AssessmentService assessmentService;
    private final StudentRepository studentRepository;

    public ReportService(AssessmentService assessmentService, StudentRepository studentRepository) {
        this.assessmentService = assessmentService;
        this.studentRepository = studentRepository;
    }

    public byte[] generatePdf(UUID studentId, Long sessionId) throws Exception {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        ExamResult result = assessmentService.calculateResult(sessionId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, baos);

        document.open();

        Font headerFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font labelFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font valueFont = new Font(Font.HELVETICA, 12);

        Paragraph header = new Paragraph("Assessment Report", headerFont);
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingAfter(20);
        document.add(header);

        Paragraph studentName = new Paragraph("Student: " + student.getFirstname() + " " + student.getLastname(), valueFont);
        studentName.setSpacingAfter(15);
        document.add(studentName);

        Paragraph mathScore = new Paragraph(
                "Math: " + result.getMathCorrect() + "/" + result.getMathTotal() +
                        " (" + String.format("%.2f", result.getMathPercentage()) + "%)", valueFont);
        mathScore.setSpacingAfter(10);
        document.add(mathScore);

        Paragraph englishScore = new Paragraph(
                "English: " + result.getEnglishCorrect() + "/" + result.getEnglishTotal() +
                        " (" + String.format("%.2f", result.getEnglishPercentage()) + "%)", valueFont);
        englishScore.setSpacingAfter(10);
        document.add(englishScore);

        Paragraph totalScore = new Paragraph(
                "Total: " + result.getTotalCorrect() + "/" + result.getTotalQuestions() +
                        " (" + String.format("%.2f", result.getTotalPercentage()) + "%)", labelFont);
        totalScore.setSpacingAfter(15);
        document.add(totalScore);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Paragraph date = new Paragraph("Completed at: " + result.getCompletedAt().format(formatter), valueFont);
        document.add(date);

        document.close();

        return baos.toByteArray();
    }
}