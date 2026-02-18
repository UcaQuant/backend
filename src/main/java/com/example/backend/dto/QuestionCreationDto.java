package com.example.backend.dto;

import com.example.backend.domain.Subject;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class QuestionCreationDto {

    private Subject subject;

    @NotBlank(message = "Question content is required")
    private String content;

    @NotNull
    @Size(min = 2, max = 6, message = "Must have between 2 and 6 options")
    private List<String> options;

    @NotNull
    @Min(0)
    @Max(5)
    private Integer correctIndex;
}
