package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentExamHistoryDto {
    private String examTitle;
    private int score;
    private int totalQuestions;
    private LocalDateTime date;
    private String reportUrl;
}
