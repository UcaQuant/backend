package com.example.backend.controller;

import com.example.backend.domain.ExamSession;
import com.example.backend.domain.SessionStatus;
import com.example.backend.exception.NotFoundException;
import com.example.backend.service.ReportService;
import com.example.backend.repository.ExamSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ExamSessionRepository sessionRepository;

    @GetMapping("/{sessionId}/download")
    public ResponseEntity<byte[]> downloadReport(@PathVariable UUID sessionId) throws Exception {

        ExamSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found"));

        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new com.example.backend.exception.ConflictException(
                    "Report is only available after the exam is completed");
        }

        byte[] pdfBytes = reportService.generatePdf(session.getStudent().getId(), sessionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        org.springframework.http.ContentDisposition contentDisposition = org.springframework.http.ContentDisposition
                .attachment()
                .filename("report_" + sessionId + ".pdf")
                .build();
        headers.setContentDisposition(contentDisposition);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}