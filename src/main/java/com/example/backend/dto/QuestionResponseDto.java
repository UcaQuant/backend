package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class QuestionResponseDto {
    private Long id;
    private String content;
    private List<String> options;
    private Integer selectedOption; // null if not answered yet
}
