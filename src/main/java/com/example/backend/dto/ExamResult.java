package com.example.backend.dto;

import java.time.LocalDateTime;

public class ExamResult {

    private final int mathCorrect;
    private final int mathTotal;
    private final double mathPercentage;

    private final int englishCorrect;
    private final int englishTotal;
    private final double englishPercentage;

    private final int totalCorrect;
    private final int totalQuestions;
    private final double totalPercentage;

    private final LocalDateTime completedAt; // add this field

    public ExamResult(int mathCorrect, int mathTotal, double mathPercentage,
                      int englishCorrect, int englishTotal, double englishPercentage,
                      LocalDateTime completedAt) { // update constructor
        this.mathCorrect = mathCorrect;
        this.mathTotal = mathTotal;
        this.mathPercentage = mathPercentage;

        this.englishCorrect = englishCorrect;
        this.englishTotal = englishTotal;
        this.englishPercentage = englishPercentage;

        this.totalCorrect = mathCorrect + englishCorrect;
        this.totalQuestions = mathTotal + englishTotal;
        this.totalPercentage = totalQuestions == 0 ? 0 : ((double) totalCorrect / totalQuestions) * 100;

        this.completedAt = completedAt;
    }

    public int getMathCorrect() { return mathCorrect; }
    public int getMathTotal() { return mathTotal; }
    public double getMathPercentage() { return mathPercentage; }

    public int getEnglishCorrect() { return englishCorrect; }
    public int getEnglishTotal() { return englishTotal; }
    public double getEnglishPercentage() { return englishPercentage; }

    public int getTotalCorrect() { return totalCorrect; }
    public int getTotalQuestions() { return totalQuestions; }
    public double getTotalPercentage() { return totalPercentage; }

    public LocalDateTime getCompletedAt() { return completedAt; } // add getter
}