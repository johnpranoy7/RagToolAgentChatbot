package com.vfu.chatbot.api;

import com.vfu.chatbot.model.ChatRequest;
import com.vfu.chatbot.model.ChatResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        //TODO: Servicecall to ChatOrchestrator
        return null;
    }
}
