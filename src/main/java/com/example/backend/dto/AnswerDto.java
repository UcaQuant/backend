package com.example.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerDto {

    @NotNull
    private Long questionId;

    // 0–3 (4 options), null not allowed here
    @NotNull
    @Min(0)
    @Max(3)
    private Integer selectedOptionIndex;
}
