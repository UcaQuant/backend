package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class QuestionPageResponse {
    private List<QuestionResponseDto> questions;
    private int totalPages;
    private int currentPage;
    private boolean isLastPage;
}
