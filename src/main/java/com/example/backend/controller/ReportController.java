package com.example.backend.controller;

import com.example.backend.domain.ExamSession;
import com.example.backend.domain.SessionStatus;
import com.example.backend.service.ReportService;
import com.example.backend.repository.ExamSessionRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final ExamSessionRepository sessionRepository;

    public ReportController(ReportService reportService, ExamSessionRepository sessionRepository) {
        this.reportService = reportService;
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/{sessionId}/download")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long sessionId) throws Exception {

        //Fetch session
        ExamSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        //Validate session status
        if (session.getStatus() != SessionStatus.COMPLETED) {
            return ResponseEntity.status(403).body(null); // forbidden if not completed
        }

        //Generate PDF
        byte[] pdfBytes = reportService.generatePdf(session.getStudent().getId(), sessionId);

        //Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "report_" + sessionId + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}