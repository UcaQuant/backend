package com.example.backend.repository;

import com.example.backend.domain.Exam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ExamRepositoryTest {

    @Autowired
    private ExamRepository examRepository;

    private Exam testExam;

    @BeforeEach
    void setUp() {
        testExam = new Exam();
        testExam.setTitle("Java Basics");
        testExam.setTimeLimitSeconds(1800);
        examRepository.save(testExam);
    }

    @Test
    void shouldSaveAndRetrieveExam() {
        Exam found = examRepository.findById(testExam.getId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getTitle()).isEqualTo("Java Basics");
        assertThat(found.getTimeLimitSeconds()).isEqualTo(1800);
    }

    @Test
    void shouldUpdateExamTitle() {
        testExam.setTitle("Advanced Java");
        examRepository.save(testExam);

        Exam updated = examRepository.findById(testExam.getId()).get();
        assertThat(updated.getTitle()).isEqualTo("Advanced Java");
    }
}