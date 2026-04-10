package com.vfu.chatbot.scheduling;

import com.vfu.chatbot.service.ChatAnalyticsCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAnalyticsCleanupScheduler {

    private final ChatAnalyticsCleanupService chatAnalyticsCleanupService;

    /**
     * Daily at 23:00 (11:00 PM) server default time zone — Spring 6-field cron: sec min hour dom month dow.
     */
    @Scheduled(cron = "${app.chat-analytics.cleanup-cron:0 0 23 * * *}")
    public void cleanupOldAnalytics() {
        log.info("Scheduled job: chat_analytics cleanup starting");
        chatAnalyticsCleanupService.deleteAnalyticsOlderThanOneWeek();
    }
}
