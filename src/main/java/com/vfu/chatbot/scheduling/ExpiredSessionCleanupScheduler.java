package com.vfu.chatbot.scheduling;

import com.vfu.chatbot.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredSessionCleanupScheduler {

    private final SessionService sessionService;

    /**
     * Wall clock: every hour at minute :00 and :30 (Spring 6-field cron: sec min hour dom month dow).
     * Override with {@code app.session.cleanup-cron}.
     */
    @Scheduled(cron = "${app.session.cleanup-cron:0 0,30 * * * *}")
    public void cleanupExpiredSessions() {
        log.info("Scheduled job: expired session cleanup starting");
        sessionService.cleanupExpiredSessions();
    }
}
