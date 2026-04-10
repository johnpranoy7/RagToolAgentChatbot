package com.vfu.chatbot.service;

import com.vfu.chatbot.repository.ChatAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatAnalyticsCleanupService {

    private final ChatAnalyticsRepository chatAnalyticsRepository;

    /**
     * Deletes analytics rows with {@code created_at} strictly before now minus 7 days (JVM default time zone).
     */
    @Transactional
    public int deleteAnalyticsOlderThanOneWeek() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime cutoff = LocalDateTime.now(zone).minusDays(7);
        int removed = chatAnalyticsRepository.deleteWhereCreatedAtBefore(cutoff);
        log.info("Chat analytics cleanup: removed {} row(s) with created_at before {} ({})", removed, cutoff, zone);
        return removed;
    }
}
