package com.vfu.chatbot.api;

import com.vfu.chatbot.model.ChatRequest;
import com.vfu.chatbot.model.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest chatRequest) {
        //TODO: Servicecall to ChatOrchestrator
        String sessionId = chatRequest.sessionId() == null ? UUID.randomUUID().toString() : chatRequest.sessionId();
        String chatbotResposeTxt = chatClient.prompt().user(u -> u.text(chatRequest.message()))
                .advisors(a->a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .toolContext(Map.of("sessionId", sessionId))
                .call().content();
        return new ChatResponse(chatbotResposeTxt, 1.0, sessionId);

    }
}
