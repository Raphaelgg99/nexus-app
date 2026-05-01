package com.nexusapp.back_end.bananapro.controller;

import com.nexusapp.back_end.bananapro.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PromptController {

    private final OpenAIService openAIService;

    @PostMapping("/melhorar-prompt")
    public Mono<ResponseEntity<Map<String, String>>> melhorar(@RequestBody Map<String, String> request) {
        String promptOriginal = request.get("prompt");

        return openAIService.melhorarPrompt(promptOriginal)
                .map(promptMelhorado -> ResponseEntity.ok(Map.of("prompt", promptMelhorado)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
