package com.example.backend.service;

import com.example.backend.domain.*;
import com.example.backend.dto.ExamResult;
import com.example.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AssessmentServiceTest {

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private StudentResponseRepository responseRepo;

    @Autowired
    private ExamSessionRepository sessionRepo;

    @Autowired
    private QuestionRepository questionRepo;

    @Autowired
    private ExamRepository examRepo;

    @Autowired
    private StudentRepository studentRepo;

    private ExamSession session;
    private Question math1, math2, english1, english2;

    @BeforeEach
    void setup() {
        // Clear previous data
        responseRepo.deleteAll();
        questionRepo.deleteAll();
        sessionRepo.deleteAll();
        examRepo.deleteAll();
        studentRepo.deleteAll();

        // Create Exam
        Exam exam = new Exam();
        exam.setTitle("Sample Exam");
        exam.setTimeLimitSeconds(3600);
        exam = examRepo.save(exam);

        // Create Student (all mandatory fields set)
        Student student = new Student();
        student.setFirstname("John");
        student.setLastname("Doe");
        student.setMobileNumber("1234567890"); //10-digit valid
        student = studentRepo.save(student);

        // Create ExamSession linked to Student and Exam
        session = new ExamSession();
        session.setExam(exam);
        session.setStudent(student);
        session = sessionRepo.save(session);

        // Create Questions linked to Exam
        math1 = new Question();
        math1.setSubject(Subject.MATH);
        math1.setCorrectIndex(0);
        math1.setExam(exam);
        math1 = questionRepo.save(math1);

        math2 = new Question();
        math2.setSubject(Subject.MATH);
        math2.setCorrectIndex(1);
        math2.setExam(exam);
        math2 = questionRepo.save(math2);

        english1 = new Question();
        english1.setSubject(Subject.ENGLISH);
        english1.setCorrectIndex(2);
        english1.setExam(exam);
        english1 = questionRepo.save(english1);

        english2 = new Question();
        english2.setSubject(Subject.ENGLISH);
        english2.setCorrectIndex(0);
        english2.setExam(exam);
        english2 = questionRepo.save(english2);

        // Create StudentResponses
        StudentResponse r1 = new StudentResponse();
        r1.setSession(session);
        r1.setQuestion(math1);
        r1.setChosenIndex(0); // correct

        StudentResponse r2 = new StudentResponse();
        r2.setSession(session);
        r2.setQuestion(math2);
        r2.setChosenIndex(0); // wrong

        StudentResponse r3 = new StudentResponse();
        r3.setSession(session);
        r3.setQuestion(english1);
        r3.setChosenIndex(2); // correct

        StudentResponse r4 = new StudentResponse();
        r4.setSession(session);
        r4.setQuestion(english2);
        r4.setChosenIndex(null); // unanswered

        responseRepo.saveAll(Arrays.asList(r1, r2, r3, r4));
    }

    @Nested
    class ResultCalculations {

        @Test
        void testCorrectVsWrongAnswers() {
            ExamResult result = assessmentService.calculateResult(session.getId());
            assertEquals(1, result.getMathCorrect(), "Math correct count should be 1");
            assertEquals(1, result.getEnglishCorrect(), "English correct count should be 1");
        }

        @Test
        void testSubjectSeparation() {
            ExamResult result = assessmentService.calculateResult(session.getId());
            assertEquals(2, result.getMathTotal(), "Math total count should be 2");
            assertEquals(2, result.getEnglishTotal(), "English total count should be 2");
        }

        @Test
        void testUnansweredQuestionsHandled() {
            ExamResult result = assessmentService.calculateResult(session.getId());
            //english2 unanswered, should not increase correct
            assertEquals(1, result.getEnglishCorrect(), "Unanswered questions should not count as correct");
        }

        @Test
        void testAccuratePercentages() {
            ExamResult result = assessmentService.calculateResult(session.getId());
            assertEquals(50.0, result.getMathPercentage(), 0.01, "Math percentage should be 50%");
            assertEquals(50.0, result.getEnglishPercentage(), 0.01, "English percentage should be 50%");
            assertEquals(50.0, result.getTotalPercentage(), 0.01, "Total percentage should be 50%");
        }

        @Test
        void testIdempotency() {
            ExamResult first = assessmentService.calculateResult(session.getId());
            ExamResult second = assessmentService.calculateResult(session.getId());
            assertEquals(first.getTotalPercentage(), second.getTotalPercentage(), 0.01, "Total percentage should be same on repeated calls");
            assertEquals(first.getMathCorrect(), second.getMathCorrect(), "Math correct should be same on repeated calls");
            assertEquals(first.getEnglishCorrect(), second.getEnglishCorrect(), "English correct should be same on repeated calls");
        }
    }
}