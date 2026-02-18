package com.example.backend.service;

import com.example.backend.domain.Exam;
import com.example.backend.domain.Question;
import com.example.backend.dto.CreateExamDto;
import com.example.backend.dto.QuestionCreationDto;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.ExamRepository;
import com.example.backend.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamAuthoringService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;

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
