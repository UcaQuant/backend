package com.example.backend.service;

import com.example.backend.domain.Exam;
import com.example.backend.domain.ExamSession;
import com.example.backend.domain.SessionStatus;
import com.example.backend.domain.Student;
import com.example.backend.dto.*;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.backend.domain.Question;
import com.example.backend.domain.StudentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class ExamService {

    private final StudentRepository studentRepository;
    private final ExamRepository examRepository;
    private final ExamSessionRepository examSessionRepository;
    private final QuestionRepository questionRepository;
    private final StudentResponseRepository studentResponseRepository;
    private final ResultService resultService;

    @Transactional
    public ExamSessionResponse startExamSession(UUID studentId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found"));

        // Find active session for this student
        var activeSessions = examSessionRepository.findByStudentIdAndStatus(studentId, SessionStatus.STARTED);
        ExamSession activeSession = activeSessions.isEmpty() ? null : activeSessions.get(0);

        if (activeSession != null) {
            return toResponse(activeSession);
        }

        // For now: pick first available exam (could be improved later)
        // TODO: Replace with real exam selection logic when exam creation is implemented
        Exam exam = examRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No active exam available"));

        ExamSession session = new ExamSession();
        session.setExam(exam);
        session.setStudent(student);
        session.setStatus(SessionStatus.STARTED);
        session.setStartTime(LocalDateTime.now());

        ExamSession saved = examSessionRepository.save(session);

        return toResponse(saved);
    }

    public QuestionPageResponse getExamQuestionsPage(Long sessionId, int page, int size) {
        ExamSession session = examSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found"));

        if (session.getStatus() != SessionStatus.STARTED) {
            throw new NotFoundException("Session is not active");
        }

        Exam exam = session.getExam();

        Pageable pageable = PageRequest.of(page, size);
        Page<Question> questionPage =
                questionRepository.findByExamId(exam.getId(), pageable);

        // Fetch existing answers for this session (map questionId -> chosenIndex)
        List<StudentResponse> responses =
                studentResponseRepository.findBySessionId(sessionId);

        Map<Long, Integer> chosenByQuestionId = responses.stream()
                .collect(Collectors.toMap(
                        r -> r.getQuestion().getId(),
                        StudentResponse::getChosenIndex,
                        (a, b) -> a
                ));

        List<QuestionResponseDto> questionDtos = questionPage.getContent().stream()
                .map(q -> new QuestionResponseDto(
                        q.getId(),
                        q.getContent(),
                        q.getOptions(),                 // correctIndex NOT included
                        chosenByQuestionId.get(q.getId())
                ))
                .toList();

        return new QuestionPageResponse(
                questionDtos,
                questionPage.getTotalPages(),
                questionPage.getNumber(),
                questionPage.isLast()
        );
    }


    private ExamSessionResponse toResponse(ExamSession session) {
        return new ExamSessionResponse(
                session.getId(),
                session.getExam().getTimeLimitSeconds(),
                session.getStartTime()
        );
    }

    @Transactional
    public void saveAnswers(Long sessionId, List<AnswerDto> answers) {

        ExamSession session = examSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found"));

        if (session.getStatus() != SessionStatus.STARTED) {
            throw new ConflictException("Session is not active");
        }

        Exam exam = session.getExam();

        for (AnswerDto dto : answers) {
            // Validate question exists and belongs to this exam
            Question question = questionRepository.findById(dto.getQuestionId())
                    .orElseThrow(() -> new BadRequestException("Invalid question id: " + dto.getQuestionId()));

            if (!question.getExam().getId().equals(exam.getId())) {
                throw new BadRequestException("Question does not belong to this exam: " + dto.getQuestionId());
            }

            // Upsert StudentResponse
            StudentResponse response = studentResponseRepository
                    .findBySessionIdAndQuestionId(sessionId, dto.getQuestionId())
                    .orElseGet(() -> {
                        StudentResponse sr = new StudentResponse();
                        sr.setSession(session);
                        sr.setQuestion(question);
                        return sr;
                    });

            response.setChosenIndex(dto.getSelectedOptionIndex());
            response.setIsCorrect(
                    question.getCorrectIndex() != null &&
                            question.getCorrectIndex().equals(dto.getSelectedOptionIndex())
            );
            response.setSubmittedAt(LocalDateTime.now());

            studentResponseRepository.save(response);
        }
    }

    @Transactional
    public ExamSubmitResponse submitExam(Long sessionId) {
        ExamSession session = examSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found"));

        if (session.getStatus() != SessionStatus.STARTED) {
            throw new ConflictException("Session already submitted or not started");
        }

        session.setStatus(SessionStatus.SUBMITTED);
        session.setEndTime(LocalDateTime.now()); // submitTime

        long answeredCount = studentResponseRepository.countAnsweredBySessionId(sessionId);
        int totalCount = session.getExam().getQuestions().size();
        int unansweredCount = totalCount - (int) answeredCount;

        return new ExamSubmitResponse(answeredCount, totalCount, unansweredCount);
    }

    @Transactional
    public ExamFinishResponse finishExam(Long sessionId) {
        ExamSession session = examSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found"));

        if (session.getStatus() != SessionStatus.SUBMITTED) {
            throw new ConflictException("Session must be SUBMITTED before finishing");
        }

        session.setStatus(SessionStatus.COMPLETED);

        String reportPath = resultService.calculateResult(sessionId);
        String downloadUrl = "/api/v1/reports/" + reportPath;

        return new ExamFinishResponse(downloadUrl);
    }


}
