package com.nexusapp.back_end.chatbot.controller;

import com.nexusapp.back_end.chatbot.model.ChatBot;
import com.nexusapp.back_end.chatbot.service.ChatBotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbot")
public class ChatBotController {

    private final ChatBotService service;

    public ChatBotController(ChatBotService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ChatBot> getStatus() {
        return ResponseEntity.ok(service.getStatus());
    }

    @PatchMapping("/toggle")
    public ResponseEntity<ChatBot> toggle() {
        return ResponseEntity.ok(service.toggleStatus());
    }
}