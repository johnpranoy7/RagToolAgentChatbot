package com.vfu.chatbot.api;

import com.vfu.chatbot.model.ChatRequest;
import com.vfu.chatbot.model.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping
    public String chat(@RequestBody ChatRequest request) {
        //TODO: Servicecall to ChatOrchestrator
        return chatClient.prompt().user(u->u.text(request.message())).call().content();
    }
}
