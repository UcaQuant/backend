package com.example.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "student_responses", uniqueConstraints = {
        @UniqueConstraint(name = "uc_session_question", columnNames = {"session_id", "question_id"})
})
@AllArgsConstructor
@NoArgsConstructor
public class StudentResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    private Integer chosenIndex;
    private Boolean isCorrect;
}