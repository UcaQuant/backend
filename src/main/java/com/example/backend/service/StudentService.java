package com.example.backend.service;

import com.example.backend.domain.Student;
import com.example.backend.dto.StudentRegistrationRequest;
import com.example.backend.dto.StudentRegistrationResponse;
import com.example.backend.exception.DuplicateMobileException;
import com.example.backend.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    @Transactional
    public StudentRegistrationResponse registerStudent(StudentRegistrationRequest request) {
        // Duplicate mobile check
        studentRepository.findStudentByMobileNumber(request.getMobileNumber())
                .ifPresent(existing -> {
                    throw new DuplicateMobileException("Student with this mobile number already exists");
                });

        Student student = new Student();
        student.setFirstname(request.getFirstName());
        student.setLastname(request.getLastName());
        student.setMobileNumber(request.getMobileNumber());

        Student saved = studentRepository.save(student);

        StudentRegistrationResponse.DataPayload payload =
                new StudentRegistrationResponse.DataPayload(
                        saved.getId().toString(),
                        "/instructions"
                );

        return new StudentRegistrationResponse(true, payload);
    }

}
