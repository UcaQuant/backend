package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentLoginRequest {
    @NotBlank(message = "Mobile number is mandatory")
    private String mobileNumber;

    @NotBlank(message = "Password is mandatory")
    private String password;
}
