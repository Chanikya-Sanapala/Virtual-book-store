package com.bookstore.controller;

import com.bookstore.dto.ChatRequest;
import com.bookstore.dto.ChatResponse;
import com.bookstore.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String aiResponse = aiService.getChatResponse(request.getMessage());
        return new ChatResponse(aiResponse);
    }
}
