package com.example.backend.service;

import org.springframework.stereotype.Service;

@Service
public class ResultService {

    public String calculateResult(Long sessionId) {
        // TODO: load session, responses, compute score, persist result, generate report
        // For now, just return a fake file name or path
        return "reports/session-" + sessionId + ".pdf";
    }
}

