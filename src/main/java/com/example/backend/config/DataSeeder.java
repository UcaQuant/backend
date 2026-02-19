package com.example.backend.config;

import com.example.backend.domain.*;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdminUser();
        seedSampleExam();
    }

    private void seedAdminUser() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setFullName("System Administrator");
            userRepository.save(admin);
            log.info("Seeded admin user (username=admin, password=admin123)");
        }

        if (userRepository.findByUsername("manager").isEmpty()) {
            AppUser manager = new AppUser();
            manager.setUsername("manager");
            manager.setPasswordHash(passwordEncoder.encode("manager123"));
            manager.setRole(Role.MANAGER);
            manager.setFullName("Default Manager");
            userRepository.save(manager);
            log.info("Seeded manager user");
        }

        if (userRepository.findByUsername("teacher").isEmpty()) {
            AppUser teacher = new AppUser();
            teacher.setUsername("teacher");
            teacher.setPasswordHash(passwordEncoder.encode("teacher123"));
            teacher.setRole(Role.TEACHER);
            teacher.setFullName("Default Teacher");
            userRepository.save(teacher);
            log.info("Seeded teacher user");
        }
    }

    private void seedSampleExam() {
        if (examRepository.count() > 0) {
            log.info("Exam data already exists, skipping seed");
            return;
        }

        Exam exam = new Exam();
        exam.setTitle("9th Grade Assessment 2024");
        exam.setTimeLimitSeconds(3600); // 1 hour
        exam = examRepository.save(exam);

        // 10 Math questions
        List<String> opts = List.of("A", "B", "C", "D");
        String[] mathQuestions = {
                "What is 2 + 2?",
                "What is 5 × 6?",
                "What is 100 ÷ 4?",
                "What is the square root of 81?",
                "What is 15% of 200?",
                "Solve: 3x = 12. What is x?",
                "What is the area of a rectangle with length 5 and width 3?",
                "What is 2³?",
                "What is the perimeter of a square with side 4?",
                "What is 7² - 5²?"
        };
        int[] mathAnswers = { 1, 2, 3, 0, 1, 3, 2, 1, 0, 3 }; // index of correct option

        for (int i = 0; i < mathQuestions.length; i++) {
            Question q = new Question();
            q.setExam(exam);
            Student student = new Student();
            student.setFirstname("John");
            student.setLastname("Doe");
            student.setMobileNumber("1234567890");
            student.setPassword("password123"); // Set default password

            Student savedStudent = studentRepository.save(student);
            q.setSubject(Subject.MATH);
            q.setContent(mathQuestions[i]);
            q.setOptions(opts);
            q.setCorrectIndex(mathAnswers[i]);
            questionRepository.save(q);
        }

        // 10 English questions
        String[] englishQuestions = {
                "Which word is a noun?",
                "Choose the correct spelling:",
                "What is the past tense of 'run'?",
                "Which sentence is grammatically correct?",
                "What does 'benevolent' mean?",
                "Choose the correct article: ___ apple",
                "What is the plural of 'child'?",
                "Which is an adjective?",
                "What does the prefix 'un-' mean?",
                "Choose the correct conjunction: I like tea ___ coffee."
        };
        int[] englishAnswers = { 0, 2, 1, 3, 0, 2, 1, 3, 0, 2 };

        for (int i = 0; i < englishQuestions.length; i++) {
            Question q = new Question();
            q.setExam(exam);
            q.setSubject(Subject.ENGLISH);
            q.setContent(englishQuestions[i]);
            q.setOptions(opts);
            q.setCorrectIndex(englishAnswers[i]);
            questionRepository.save(q);
        }

        log.info("Seeded sample exam '{}' with 20 questions (10 Math + 10 English)", exam.getTitle());
    }
}
