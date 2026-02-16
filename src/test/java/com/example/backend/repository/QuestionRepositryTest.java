package com.example.backend.repository;

import com.example.backend.domain.Exam;
import com.example.backend.domain.Question;
import com.example.backend.domain.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuestionRepositoryTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamRepository examRepository;

    private Exam sharedExam;

    @BeforeEach
    void initData() {
        // Create the parent Exam once for all tests in this class
        sharedExam = new Exam();
        sharedExam.setTitle("Core Java");
        sharedExam.setTimeLimitSeconds(3600);
        examRepository.save(sharedExam);

        // Instantiate test questions using the shared exam
        Question q1 = createQuestion("What is a Class?", Subject.ENGLISH, List.of("Object", "Template", "Method"), 1);
        Question q2 = createQuestion("2 + 2?", Subject.MATH, List.of("3", "4", "5"), 1);

        questionRepository.saveAll(List.of(q1, q2));
    }

    // Helper method to avoid repetitive object creation
    private Question createQuestion(String content, Subject subject, List<String> options, int correctIdx) {
        Question q = new Question();
        q.setExam(sharedExam);
        q.setSubject(subject);
        q.setContent(content);
        q.setOptions(options);
        q.setCorrectIndex(correctIdx);
        return q;
    }

    @Test
    void testJsonSerializationAndDeserialization() {
        List<Question> questions = questionRepository.findByExamIdOrderById(sharedExam.getId());

        assertThat(questions.get(0).getOptions())
                .as("Check if options list is correctly deserialized from JSON")
                .containsExactly("Object", "Template", "Method");
    }

    @Test
    void testCountByExamId() {
        long count = questionRepository.countByExamId(sharedExam.getId());
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testPaginationUsingPageable() {
        Page<Question> firstPage = questionRepository.findByExamId(
                sharedExam.getId(),
                PageRequest.of(0, 1) // Request 1st page with only 1 item
        );

        assertThat(firstPage.getContent()).hasSize(1);
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
    }

    @Test
    void testFindByExamIdOrderById() {
        List<Question> questions = questionRepository.findByExamIdOrderById(sharedExam.getId());

        assertThat(questions).hasSize(2);
        // Verify IDs are in ascending order
        assertThat(questions.get(0).getId()).isLessThan(questions.get(1).getId());
    }
}