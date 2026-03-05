package com.vfu.chatbot.api;

import com.vfu.chatbot.model.ChatRequest;
import com.vfu.chatbot.model.ChatResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest chatRequest, HttpServletRequest httpRequest,
                                             HttpServletResponse httpResponse) {
        // 1. Get sessionId (body/header fallback)
        String sessionId = chatRequest.sessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            // Set sessionId in response header
            httpResponse.setHeader("X-Session-ID", sessionId);
        }

        // 2. CSRF Protection (Spring Security)
        String csrfToken = extractCsrfToken(httpRequest);
//        validateCsrf(csrfToken, request.csrfToken());

        //TODO: Servicecall to ChatOrchestrator
//        String sessionId = chatRequest.sessionId() == null ? UUID.randomUUID().toString() : chatRequest.sessionId();
        String finalSessionId = sessionId;
        String rawResponse = chatClient.prompt().user(u -> u.text(chatRequest.message()))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalSessionId))
                .toolContext(Map.of("sessionId", sessionId))
                .call().content();

        // 2. ✅ PARSE CONFIDENCE & SOURCE (your exact format)
        String content = extractContent(rawResponse);
        double confidence = extractConfidence(rawResponse);
        String source = extractSource(rawResponse);

        ChatResponse chatResponse = new ChatResponse(content, confidence, source, sessionId);
        return ResponseEntity.ok()
                .header("X-Session-ID", sessionId)  // Always return
                .body(chatResponse);

    }

    private String extractCsrfToken(HttpServletRequest request) {
        return (String) request.getAttribute("_csrf");
    }

    private String extractContent(String rawResponse) {
        // Everything before **CONFIDENCE:**
        String[] parts = rawResponse.split("\\*\\*CONFIDENCE:", 2);
        if (parts.length > 0) {
            return parts[0].trim().replaceAll("^\\*\\*ANSWER:\\*\\*", "").trim();
        }
        return rawResponse;
    }

    private double extractConfidence(String rawResponse) {
        Pattern pattern = Pattern.compile("(?i)\\*\\*CONFIDENCE:\\s*([0-9]\\.[0-9]{2})");
        Matcher matcher = pattern.matcher(rawResponse);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0.5;
            }
        }
        return 0.5;
    }

    private String extractSource(String rawResponse) {
        Pattern pattern = Pattern.compile("(?i)\\*\\*SOURCE:\\s*([^\\n]+)");
        Matcher matcher = pattern.matcher(rawResponse);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "unknown";
    }

}
