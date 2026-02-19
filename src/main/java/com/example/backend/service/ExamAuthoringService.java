package com.example.backend.service;

import com.example.backend.domain.Exam;
import com.example.backend.domain.Question;
import com.example.backend.dto.CreateExamDto;
import com.example.backend.dto.QuestionCreationDto;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.ExamRepository;
import com.example.backend.repository.ExamSessionRepository;
import com.example.backend.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamAuthoringService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ExamSessionRepository examSessionRepository;
    private final AssessmentService assessmentService;

    @Transactional(readOnly = true)
    public List<com.example.backend.dto.TeacherStudentResultDto> getAllStudentResults() {
        return examSessionRepository.findAll().stream()
                .filter(session -> session.getStatus() == com.example.backend.domain.SessionStatus.COMPLETED)
                .map(session -> {
                    com.example.backend.dto.ExamResult result = assessmentService.calculateResult(session.getId());
                    String studentName = session.getStudent().getFirstname() + " " + session.getStudent().getLastname();
                    return new com.example.backend.dto.TeacherStudentResultDto(
                            session.getId(),
                            studentName,
                            session.getExam().getTitle(),
                            result.getTotalCorrect(),
                            result.getTotalQuestions(),
                            result.getTotalPercentage(),
                            session.getSubmitTime() != null ? session.getSubmitTime() : java.time.LocalDateTime.now());
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    @Transactional
    public Exam createExam(CreateExamDto dto) {
        Exam exam = new Exam();
        exam.setTitle(dto.getTitle());
        exam.setTimeLimitSeconds(dto.getTimeLimitSeconds());
        return examRepository.save(exam);
    }

    @Transactional
    public Question addQuestion(Long examId, QuestionCreationDto dto) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new NotFoundException("Exam not found: " + examId));

        Question question = new Question();
        question.setExam(exam);
        question.setSubject(dto.getSubject());
        question.setContent(dto.getContent());
        question.setOptions(dto.getOptions());
        question.setCorrectIndex(dto.getCorrectIndex());

        return questionRepository.save(question);
    }

    @Transactional
    public Question updateQuestion(Long questionId, QuestionCreationDto dto) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found: " + questionId));

        question.setSubject(dto.getSubject());
        question.setContent(dto.getContent());
        question.setOptions(dto.getOptions());
        question.setCorrectIndex(dto.getCorrectIndex());

        return questionRepository.save(question);
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new NotFoundException("Question not found: " + questionId);
        }
        questionRepository.deleteById(questionId);
    }
}
