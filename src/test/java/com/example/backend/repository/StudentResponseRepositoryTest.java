package com.example.backend.repository;

import com.example.backend.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StudentResponseRepositoryTest {

    @Autowired private StudentResponseRepository responseRepo;
    @Autowired private ExamSessionRepository sessionRepo;
    @Autowired private QuestionRepository questionRepo;
    @Autowired private StudentRepository studentRepo;
    @Autowired private ExamRepository examRepo;

    private ExamSession savedSession;
    private Question savedQuestion;

    @BeforeEach
    void setUp() {
        // 1. Create and Save Student (Required for Session)
        Student student = new Student();
        student.setFirstname("John");
        student.setLastname("Doe");
        student.setMobileNumber("1234567890");
        student = studentRepo.save(student);

        // 2. Create and Save Exam (Required for Question and Session)
        Exam exam = new Exam();
        exam.setTitle("Java Persistence Exam");
        exam = examRepo.save(exam);

        // 3. Create and Save Question (Required for Response)
        Question question = new Question();
        question.setExam(exam);
        question.setContent("What is JPA?");
        question.setSubject(Subject.MATH); // Assuming Subject enum exists
        question.setOptions(List.of("API", "Library", "Framework"));
        question.setCorrectIndex(0);
        savedQuestion = questionRepo.save(question);

        // 4. Create and Save Session (Required for Response)
        ExamSession session = new ExamSession();
        session.setStudent(student);
        session.setExam(exam);
        session.setStatus(SessionStatus.STARTED);
        savedSession = sessionRepo.save(session);
    }

    @Test
    void testUniqueConstraint_PreventsDuplicateAnswersPerQuestionPerSession() {
        // First response - should save successfully
        StudentResponse r1 = new StudentResponse();
        r1.setSession(savedSession);
        r1.setQuestion(savedQuestion);
        r1.setChosenIndex(1);
        responseRepo.saveAndFlush(r1);

        // Second response - same session and same question
        StudentResponse r2 = new StudentResponse();
        r2.setSession(savedSession);
        r2.setQuestion(savedQuestion);
        r2.setChosenIndex(2);

        // ASSERT: Database should block this due to @UniqueConstraint
        assertThrows(DataIntegrityViolationException.class, () -> {
            responseRepo.saveAndFlush(r2);
        });
    }

    @Test
    void testCountAnsweredBySessionId() {
        // Setup: save one answered response
        StudentResponse response = new StudentResponse();
        response.setSession(savedSession);
        response.setQuestion(savedQuestion);
        response.setChosenIndex(1);
        responseRepo.saveAndFlush(response);

        // ACT
        long count = responseRepo.countAnsweredBySessionId(savedSession.getId());

        // ASSERT
        assertThat(count).isEqualTo(1);
    }
}