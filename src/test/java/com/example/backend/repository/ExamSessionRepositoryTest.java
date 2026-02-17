package com.example.backend.repository;

import com.example.backend.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ExamSessionRepositoryTest {

    @Autowired private ExamSessionRepository sessionRepo;
    @Autowired private StudentResponseRepository responseRepo;
    @Autowired private QuestionRepository questionRepo;

    // TestEntityManager is the secret sauce for reliable JPA tests
    @Autowired private TestEntityManager entityManager;

    private Long sessionId;
    private Long questionId;

    @BeforeEach
    void setUp() {
        // 1. Clear existing data to prevent ID collisions or constraint issues
        responseRepo.deleteAllInBatch();
        sessionRepo.deleteAllInBatch();

        // 2. Clear the persistence context so we start with a blank slate
        entityManager.flush();
        entityManager.clear();

        // 3. Persist dependencies using the entityManager
        Student student = new Student();
        student.setFirstname("John");
        student.setLastname("Doe");
        student.setMobileNumber("1234567890");
        entityManager.persist(student);

        Exam exam = new Exam();
        exam.setTitle("Spring Boot 101");
        entityManager.persist(exam);

        Question question = new Question();
        question.setExam(exam);
        question.setContent("Question 1");
        question.setSubject(Subject.MATH);
        question.setOptions(List.of("A", "B"));
        question.setCorrectIndex(0);
        entityManager.persist(question);
        this.questionId = question.getId();

        ExamSession session = new ExamSession();
        session.setStudent(student);
        session.setExam(exam);
        session.setStatus(SessionStatus.STARTED);
        entityManager.persist(session);
        this.sessionId = session.getId();

        // 4. Critical: Push everything to DB and clear the cache
        // This ensures the entities are no longer "transient"
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void testCascadeDelete() {
        // Re-fetch entities to ensure they are MANAGED in this specific test transaction
        ExamSession session = sessionRepo.findById(sessionId).orElseThrow();
        Question question = questionRepo.findById(questionId).orElseThrow();

        StudentResponse res = new StudentResponse();
        res.setSession(session);
        res.setQuestion(question);
        responseRepo.saveAndFlush(res);

        // Perform delete
        sessionRepo.delete(session);
        sessionRepo.flush();

        // Assert response was deleted via cascade (requires CascadeType.REMOVE on ExamSession)
        assertThat(responseRepo.findBySessionId(sessionId)).isEmpty();
    }

    @Test
    void testUniqueConstraint() {
        ExamSession session = sessionRepo.findById(sessionId).orElseThrow();
        Question question = questionRepo.findById(questionId).orElseThrow();

        StudentResponse r1 = new StudentResponse();
        r1.setSession(session);
        r1.setQuestion(question);
        responseRepo.saveAndFlush(r1);

        StudentResponse r2 = new StudentResponse();
        r2.setSession(session);
        r2.setQuestion(question);

        // DataIntegrityViolationException occurs on flush when the DB constraint is hit
        assertThrows(DataIntegrityViolationException.class, () -> {
            responseRepo.saveAndFlush(r2);
        });
    }

    @Test
    void testCountAnswered() {
        ExamSession session = sessionRepo.findById(sessionId).orElseThrow();
        Question question = questionRepo.findById(questionId).orElseThrow();

        StudentResponse res = new StudentResponse();
        res.setSession(session);
        res.setQuestion(question);
        res.setChosenIndex(1);
        responseRepo.saveAndFlush(res);

        long count = responseRepo.countAnsweredBySessionId(sessionId);
        assertThat(count).isEqualTo(1);
    }
}