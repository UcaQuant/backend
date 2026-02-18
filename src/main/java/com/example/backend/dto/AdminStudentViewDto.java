package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminStudentViewDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private LocalDateTime registeredAt;
    private String examStatus; // "NOT_TAKEN", "IN_PROGRESS", "COMPLETED"
}
