package com.example.backend.repository;

import com.example.backend.domain.Student;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.annotation.Commit;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    private Student johnStudent;

    @BeforeEach
    void setup() {
        studentRepository.deleteAll();
        johnStudent = new Student();
        johnStudent.setFirstname("John");
        johnStudent.setLastname("Doe");
        johnStudent.setMobileNumber("1234567890");
        johnStudent = studentRepository.saveAndFlush(johnStudent); // flush ensures createdAt is populated
    }

    @Test
    @DisplayName("Check shared student has UUID and createdAt")
    void testSharedStudentProperties() {
        assertThat(johnStudent).isNotNull();
        assertThat(johnStudent.getId()).isNotNull();
        assertThat(johnStudent.getId()).isInstanceOf(UUID.class);
        assertThat(johnStudent.getCreatedAt()).isNotNull();
        assertThat(johnStudent.getFirstname()).isEqualTo("John");
        assertThat(johnStudent.getLastname()).isEqualTo("Doe");
        assertThat(johnStudent.getMobileNumber()).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("Find the shared student by mobile number")
    void testFindSharedStudentByMobileNumber() {
        Optional<Student> retrieved = studentRepository.findStudentByMobileNumber("1234567890");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getFirstname()).isEqualTo("John");
    }

    @Test
    @DisplayName("Fail to save student with invalid mobile number")
    void testInvalidMobileNumber() {
        Student student = new Student();
        student.setFirstname("Bob");
        student.setLastname("Brown");
        student.setMobileNumber("123"); // invalid

        assertThrows(ConstraintViolationException.class, () -> {
            studentRepository.saveAndFlush(student);
        });
    }

    @Test
    @DisplayName("Fail to save student with blank firstname or lastname")
    void testInvalidName() {
        Student student = new Student();
        student.setFirstname(""); // blank
        student.setLastname("");  // blank
        student.setMobileNumber("1234567890");

        assertThrows(ConstraintViolationException.class, () -> {
            studentRepository.saveAndFlush(student);
        });
    }
}