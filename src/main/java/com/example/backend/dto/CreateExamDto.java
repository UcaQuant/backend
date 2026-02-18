package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateExamDto {
    @NotBlank(message = "Title is required")
    private String title;

    @Positive(message = "Time limit must be positive")
    private int timeLimitSeconds;
}
