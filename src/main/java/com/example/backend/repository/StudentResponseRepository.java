package com.example.backend.repository;

import com.example.backend.domain.StudentResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentResponseRepository extends JpaRepository<StudentResponse, Long> {

    List<StudentResponse> findBySessionId(Long sessionId);

    Optional<StudentResponse> findBySessionIdAndQuestionId(Long sessionId, Long questionId);

    @Query("SELECT COUNT(r) FROM StudentResponse r WHERE r.session.id = :sessionId AND r.chosenIndex IS NOT NULL")
    long countAnsweredBySessionId(@Param("sessionId") Long sessionId);
}