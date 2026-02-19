package com.example.backend.service;

import com.example.backend.domain.Student;
import com.example.backend.dto.ExamResult;
import com.example.backend.repository.StudentRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceAcceptanceTest {

        @Mock
        private AssessmentService assessmentService;

        @Mock
        private StudentRepository studentRepository;

        @InjectMocks
        private ReportService reportService;

        @Test
        void shouldGenerateValidPdfMeetingAcceptanceCriteria() throws Exception {

                UUID studentId = UUID.randomUUID();
                UUID sessionId = UUID.randomUUID();

                Student student = new Student();
                student.setId(studentId);
                student.setFirstname("John");
                student.setLastname("Doe");

                ExamResult result = new ExamResult(
                                1, 2, 50.0,
                                1, 2, 50.0,
                                LocalDateTime.now());

                when(studentRepository.findById(studentId))
                                .thenReturn(Optional.of(student));
                when(assessmentService.calculateResult(sessionId))
                                .thenReturn(result);

                byte[] pdf = reportService.generatePdf(studentId, sessionId);

                // PDF generated
                assertNotNull(pdf);
                assertTrue(pdf.length > 0);

                // Size < 500KB
                assertTrue(pdf.length < 500_000);

                // Proper format
                assertEquals("%PDF", new String(pdf, 0, 4));

                // Extract text properly
                PDDocument document = PDDocument.load(new ByteArrayInputStream(pdf));
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                document.close();

                assertTrue(text.contains("Assessment Report"));
                assertTrue(text.contains("John Doe"));
                assertTrue(text.contains("Math: 1/2 (50.00%)"));
                assertTrue(text.contains("English: 1/2 (50.00%)"));
                assertTrue(text.contains("Total: 2/4 (50.00%)"));
                assertTrue(text.contains("Completed at:"));
        }
}