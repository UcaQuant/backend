package com.example.backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Data
@Table(name = "students")
@AllArgsConstructor
@NoArgsConstructor
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "First name is mandatory")
    @Size(min = 2, max = 50)
    private String firstname;

    @NotBlank(message = "Last name is mandatory")
    @Size(min = 2, max = 50)
    private String lastname;

    @NotBlank(message = "Mobile number is mandatory")
    @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    @NotBlank(message = "Password is mandatory")
    private String password;

    @CreationTimestamp
    private LocalDateTime createdAt;

}
