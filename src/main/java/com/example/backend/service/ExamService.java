package com.example.backend.service;

import com.example.backend.domain.Exam;
import com.example.backend.domain.ExamSession;
import com.example.backend.domain.Question;
import com.example.backend.domain.SessionStatus;
import com.example.backend.domain.Student;
import com.example.backend.domain.StudentResponse;
import com.example.backend.dto.*;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

        private final StudentRepository studentRepository;
        private final ExamRepository examRepository;
        private final ExamSessionRepository examSessionRepository;
        private final QuestionRepository questionRepository;
        private final StudentResponseRepository studentResponseRepository;
        private final AssessmentService assessmentService;

        public List<StudentExamDto> getAllStudentExams() {
                return examRepository.findAll().stream()
                                .map(e -> new StudentExamDto(e.getId(), e.getTitle(), e.getTimeLimitSeconds()))
                                .collect(Collectors.toList());
        }

        @Transactional
        public ExamSessionResponse startExamSession(UUID studentId, Long examId) {

                Student student = studentRepository.findById(studentId)
                                .orElseThrow(() -> new NotFoundException("Student not found"));

                if (examId == null) {
                        throw new BadRequestException("Exam ID is required");
                }

                // Return existing active session for this exam if present
                List<ExamSession> activeSessions = examSessionRepository
                                .findByStudentIdAndStatus(studentId, SessionStatus.STARTED);

                // If student has ANY active session for the requested exam, return it.
                // Or if we want to restrict to ONE active session at a time globally:
                if (!activeSessions.isEmpty()) {
                        ExamSession existing = activeSessions.get(0);
                        // Verify if it matches the requested exam?
                        // Ideally we should probably force finish or return the existing one.
                        // For simplicity, if they have an active session, we return it.
                        // But if they want to start a Different exam, and have one running?
                        // Let's assume one active session at a time for now.
                        return toResponse(existing);
                }

                Exam exam = examRepository.findById(examId)
                                .orElseThrow(() -> new NotFoundException("Exam not found"));

                ExamSession session = new ExamSession();
                session.setExam(exam);
                session.setStudent(student);
                session.setStatus(SessionStatus.STARTED);
                session.setStartTime(LocalDateTime.now());

                ExamSession saved = examSessionRepository.save(session);
                return toResponse(saved);
        }

        public QuestionPageResponse getExamQuestionsPage(UUID sessionId, int page, int size) {
                ExamSession session = examSessionRepository.findById(sessionId)
                                .orElseThrow(() -> new NotFoundException("Session not found"));

                if (session.getStatus() != SessionStatus.STARTED) {
                        throw new ConflictException("Session is not active");
                }

                Exam exam = session.getExam();
                Pageable pageable = PageRequest.of(page, Math.min(size, 20));
                Page<Question> questionPage = questionRepository.findByExamId(exam.getId(), pageable);

                // Fetch existing answers for this session
                List<StudentResponse> responses = studentResponseRepository.findBySessionId(sessionId);
                Map<Long, Integer> chosenByQuestionId = responses.stream()
                                .collect(Collectors.toMap(
                                                r -> r.getQuestion().getId(),
                                                StudentResponse::getChosenIndex,
                                                (a, b) -> a));

                List<QuestionResponseDto> questionDtos = questionPage.getContent().stream()
                                .map(q -> new QuestionResponseDto(
                                                q.getId(),
                                                q.getContent(),
                                                q.getOptions(), // correctIndex NOT included
                                                chosenByQuestionId.get(q.getId())))
                                .toList();

                return new QuestionPageResponse(
                                questionDtos,
                                questionPage.getTotalPages(),
                                questionPage.getNumber(),
                                questionPage.isLast());
        }

        @Transactional
        public void saveAnswers(UUID sessionId, List<AnswerDto> answers) {

                ExamSession session = examSessionRepository.findById(sessionId)
                                .orElseThrow(() -> new NotFoundException("Session not found"));

                if (session.getStatus() != SessionStatus.STARTED) {
                        throw new ConflictException("Session is not active");
                }

                Exam exam = session.getExam();

                for (AnswerDto dto : answers) {
                        Question question = questionRepository.findById(dto.getQuestionId())
                                        .orElseThrow(() -> new BadRequestException(
                                                        "Invalid question id: " + dto.getQuestionId()));

                        if (!question.getExam().getId().equals(exam.getId())) {
                                throw new BadRequestException(
                                                "Question does not belong to this exam: " + dto.getQuestionId());
                        }

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
                                                        question.getCorrectIndex()
                                                                        .equals(dto.getSelectedOptionIndex()));
                        response.setSubmittedAt(LocalDateTime.now());
                        studentResponseRepository.save(response);
                }
        }

        @Transactional
        public ExamSubmitResponse submitExam(UUID sessionId) {
                ExamSession session = examSessionRepository.findById(sessionId)
                                .orElseThrow(() -> new NotFoundException("Session not found"));

                if (session.getStatus() != SessionStatus.STARTED) {
                        throw new ConflictException("Session already submitted or not started");
                }

                session.setStatus(SessionStatus.SUBMITTED);
                session.setSubmitTime(LocalDateTime.now());
                examSessionRepository.save(session); // FIX: was missing

                long answeredCount = studentResponseRepository.countAnsweredBySessionId(sessionId);
                int totalCount = (int) questionRepository.countByExamId(session.getExam().getId());
                int unansweredCount = totalCount - (int) answeredCount;

                return new ExamSubmitResponse(answeredCount, totalCount, unansweredCount);
        }

        @Transactional
        public ExamFinishResponse finishExam(UUID sessionId) {
                ExamSession session = examSessionRepository.findById(sessionId)
                                .orElseThrow(() -> new NotFoundException("Session not found"));

                if (session.getStatus() != SessionStatus.SUBMITTED) {
                        throw new ConflictException("Session must be SUBMITTED before finishing");
                }

                session.setStatus(SessionStatus.COMPLETED);
                examSessionRepository.save(session);

                // Trigger grading (idempotent – can be called again on PDF download)
                assessmentService.calculateResult(sessionId);

                String downloadUrl = "/api/v1/reports/" + sessionId + "/download";
                return new ExamFinishResponse(downloadUrl);
        }

        public ExamResult getExamResult(UUID sessionId) {
                ExamSession session = examSessionRepository.findById(sessionId)
                                .orElseThrow(() -> new NotFoundException("Session not found"));

                return assessmentService.calculateResult(sessionId);
        }

        private ExamSessionResponse toResponse(ExamSession session) {
                return new ExamSessionResponse(
                                session.getId(),
                                session.getExam().getTimeLimitSeconds(),
                                session.getStartTime());
        }
}
