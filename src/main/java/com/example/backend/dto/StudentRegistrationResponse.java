package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentRegistrationResponse {
    private boolean success;
    private DataPayload data;

    @Data
    @AllArgsConstructor
    public static class DataPayload {
        private String studentId;
        private String nextAction;
    }
}
