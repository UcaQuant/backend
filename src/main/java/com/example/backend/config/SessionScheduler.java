package com.example.backend.config;

import com.example.backend.domain.ExamSession;
import com.example.backend.domain.SessionStatus;
import com.example.backend.repository.ExamSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionScheduler {

    private final ExamSessionRepository examSessionRepository;

    /**
     * Runs at 2 AM daily. Expires any STARTED sessions older than 24 hours.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void expireAbandonedSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<ExamSession> abandoned = examSessionRepository
                .findByStatusAndStartTimeBefore(SessionStatus.STARTED, cutoff);

        if (abandoned.isEmpty()) {
            log.info("Session expiry job: no abandoned sessions found");
            return;
        }

        abandoned.forEach(session -> session.setStatus(SessionStatus.EXPIRED));
        examSessionRepository.saveAll(abandoned);
        log.info("Session expiry job: expired {} abandoned session(s)", abandoned.size());
    }
}
