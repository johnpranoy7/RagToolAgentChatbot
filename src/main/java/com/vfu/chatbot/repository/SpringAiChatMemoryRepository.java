package com.vfu.chatbot.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * JDBC access to Spring AI's {@code spring_ai_chat_memory} table (see
 * {@code schema-postgresql.sql} in {@code spring-ai-model-chat-memory-repository-jdbc}).
 * This table is not mapped as a JPA entity.
 */
@Repository
public class SpringAiChatMemoryRepository {

    private static final String DELETE_BEFORE_TIMESTAMP = """
            DELETE FROM spring_ai_chat_memory WHERE "timestamp" < ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public SpringAiChatMemoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @param cutoff exclusive upper bound; rows with {@code timestamp} strictly before this are removed
     * @return number of rows deleted
     */
    public int deleteWhereTimestampBefore(LocalDateTime cutoff) {
        return jdbcTemplate.update(DELETE_BEFORE_TIMESTAMP, cutoff);
    }
}
