package com.vfu.chatbot.ai;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PolicyRagService {

    private final PgVectorStore vectorStore;
    private final ChatClient queryRewriteClient;

    public PolicyRagService(PgVectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.queryRewriteClient = chatClientBuilder.build();
    }

    public String searchPolicy(String userQuestion) {
        log.info("PolicyRagService: received question='{}'", userQuestion);
        String rewrittenQuery = rewritePolicyQueryWithLlm(userQuestion);
        String finalQuery = rewrittenQuery.isBlank() ? userQuestion : rewrittenQuery;
        if (rewrittenQuery.isBlank()) {
            log.info("PolicyRagService: rewrite empty/fallback, using raw query");
        } else {
            log.info("PolicyRagService: rewritten query='{}'", rewrittenQuery);
        }

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(finalQuery).topK(6).build()
        );
        log.info("PolicyRagService: primary search hits={}", results.size());
        if (results.isEmpty() && !finalQuery.equalsIgnoreCase(userQuestion)) {
            // Fallback to original phrasing only if rewritten retrieval misses completely.
            log.info("PolicyRagService: rewritten query had 0 hits, retrying with raw query");
            results = vectorStore.similaritySearch(
                    SearchRequest.builder().query(userQuestion).topK(6).build()
            );
            log.info("PolicyRagService: raw-query fallback hits={}", results.size());
        }
        log.info("PolicyRagService: final query used='{}'", finalQuery);

        if (results.isEmpty()) {
            log.info("PolicyRagService: no policy match after retries");
            return "NO_POLICY_MATCH";
        }
        return results.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private String rewritePolicyQueryWithLlm(String rawQuery) {
        String rewritePrompt = """
                Rewrite this user question into one short hotel-policy retrieval query.
                Constraints:
                - Preserve the core entity words from the user question (example: "security deposit" must stay).
                - Keep all intent facets present in the question (amount, required/requirement, per person/per head, refundable, timeline).
                - Include "policy" or "requirement" anchor terms.
                - Prefer neutral policy terms; do not drift to unrelated commercial wording.
                - Max 12 words.
                Return only the rewritten query text.
                User question: %s
                """.formatted(rawQuery);
        try {
            String rewritten = queryRewriteClient.prompt().user(rewritePrompt).call().content();
            if (rewritten == null) {
                log.warn("PolicyRagService: rewrite model returned null content");
                return "";
            }
            return rewritten.replace("\"", "").trim();
        } catch (Exception ex) {
            log.warn("PolicyRagService: query rewrite failed: {}", ex.getMessage());
            return "";
        }
    }
}
