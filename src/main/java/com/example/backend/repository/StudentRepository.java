package com.example.backend.repository;

import com.example.backend.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findStudentByMobileNumber(String mobileNumber);
    long countByCreatedAtAfter(LocalDateTime dateTime);
}
