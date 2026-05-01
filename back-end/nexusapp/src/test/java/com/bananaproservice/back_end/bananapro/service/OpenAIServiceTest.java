package com.nexusapp.back_end.bananapro.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAIServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Test
    void melhorarPromptShouldReturnJoinedStreamedContent() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()
                .system(anyString())
                .user("logo na caneca")
                .stream()
                .content())
                .thenReturn(Flux.just("Photorealistic ", "mug mockup"));
        OpenAIService service = new OpenAIService(chatClientBuilder);

        StepVerifier.create(service.melhorarPrompt("logo na caneca"))
                .expectNext("Photorealistic mug mockup")
                .verifyComplete();
    }

    @Test
    void melhorarPromptShouldReturnErrorWhenPromptIsBlank() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        OpenAIService service = new OpenAIService(chatClientBuilder);

        StepVerifier.create(service.melhorarPrompt("   "))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
