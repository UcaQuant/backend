package com.example.backend.service;

import com.example.backend.domain.StudentResponse;
import com.example.backend.domain.Subject;
import com.example.backend.dto.ExamResult;
import com.example.backend.repository.StudentResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssessmentService {

    private final StudentResponseRepository responseRepo;

    public AssessmentService(StudentResponseRepository responseRepo) {
        this.responseRepo = responseRepo;
    }

    @Transactional(readOnly = true)
    public ExamResult calculateResult(Long sessionId) {

        //Fetch all responses for this session
        List<StudentResponse> responses = responseRepo.findBySessionId(sessionId);

        int mathTotal = 0, mathCorrect = 0;
        int englishTotal = 0, englishCorrect = 0;

        //Loop over each response
        for (StudentResponse response : responses) {
            if (response.getQuestion() == null) continue;

            boolean isCorrect = response.getChosenIndex() != null &&
                    response.getChosenIndex().equals(response.getQuestion().getCorrectIndex());

            //Track counts per subject
            if (response.getQuestion().getSubject() == Subject.MATH) {
                mathTotal++;
                if (isCorrect) mathCorrect++;
            } else if (response.getQuestion().getSubject() == Subject.ENGLISH) {
                englishTotal++;
                if (isCorrect) englishCorrect++;
            }
        }

        //Calculate percentages
        double mathPercentage = mathTotal == 0 ? 0 : ((double) mathCorrect / mathTotal) * 100;
        double englishPercentage = englishTotal == 0 ? 0 : ((double) englishCorrect / englishTotal) * 100;

        LocalDateTime completedAt = LocalDateTime.now();
        //Return DTO including total score
        return new ExamResult(
                mathCorrect, mathTotal, mathPercentage,
                englishCorrect, englishTotal, englishPercentage,
                completedAt

        );
    }
}