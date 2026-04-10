package com.vfu.chatbot.service;

import com.vfu.chatbot.repository.SpringAiChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Removes stale rows from {@code spring_ai_chat_memory}. Retention: keep messages from
 * yesterday (calendar) and today; delete anything strictly before
 * start of yesterday in the JVM default time zone.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpringAiChatMemoryCleanupService {

    private final SpringAiChatMemoryRepository springAiChatMemoryRepository;

    /**
     * Deletes rows where {@code timestamp} &lt; start of yesterday (00:00) in {@link ZoneId#systemDefault()}.
     */
    @Transactional
    public int deleteMemoryOlderThanStartOfYesterday() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate yesterday = LocalDate.now(zone).minusDays(1);
        LocalDateTime cutoff = yesterday.atStartOfDay();
        int removed = springAiChatMemoryRepository.deleteWhereTimestampBefore(cutoff);
        log.info("Spring AI chat memory cleanup: removed {} row(s) with timestamp before {} ({})", removed, cutoff, zone);
        return removed;
    }
}
