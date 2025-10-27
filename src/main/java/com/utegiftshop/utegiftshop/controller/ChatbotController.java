package com.utegiftshop.controller;

import com.utegiftshop.dto.request.ChatRequest;
import com.utegiftshop.dto.response.ChatResponse;
import com.utegiftshop.service.ChatbotService; // Giả sử bạn tạo service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping
    public ResponseEntity<ChatResponse> handleChat(@RequestBody ChatRequest chatRequest) {
        String replyMessage = chatbotService.getChatReply(chatRequest.getMessage());

        ChatResponse response = new ChatResponse();
        response.setReply(replyMessage);

        return ResponseEntity.ok(response);
    }
}