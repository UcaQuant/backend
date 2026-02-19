package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherStudentResultDto {
    private UUID sessionId;
    private String studentName;
    private String examTitle;
    private int score;
    private int totalQuestions;
    private double percentage;
    private LocalDateTime date;
}
