package com.nexusapp.back_end.chatbot.service;

import com.nexusapp.back_end.chatbot.model.ChatBot;
import com.nexusapp.back_end.chatbot.repository.ChatBotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatBotServiceTest {

    @Mock
    private ChatBotRepository repository;

    @Test
    void getStatusShouldReturnStoredConfiguration() {
        ChatBotService service = new ChatBotService(repository);
        ChatBot config = chatBot(true);
        when(repository.findById(1L)).thenReturn(Optional.of(config));

        ChatBot result = service.getStatus();

        assertSame(config, result);
    }

    @Test
    void getStatusShouldThrowWhenConfigurationDoesNotExist() {
        ChatBotService service = new ChatBotService(repository);
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, service::getStatus);
    }

    @Test
    void toggleStatusShouldDeactivateWhenCurrentlyActive() {
        ChatBotService service = new ChatBotService(repository);
        ChatBot config = chatBot(true);
        when(repository.findById(1L)).thenReturn(Optional.of(config));
        when(repository.save(config)).thenReturn(config);

        ChatBot result = service.toggleStatus();

        assertFalse(result.isActive());
        verify(repository).save(config);
    }

    @Test
    void toggleStatusShouldActivateWhenCurrentlyInactive() {
        ChatBotService service = new ChatBotService(repository);
        ChatBot config = chatBot(false);
        when(repository.findById(1L)).thenReturn(Optional.of(config));
        when(repository.save(config)).thenReturn(config);

        ChatBot result = service.toggleStatus();

        assertTrue(result.isActive());
        verify(repository).save(config);
    }

    private ChatBot chatBot(boolean active) {
        ChatBot chatBot = new ChatBot();
        chatBot.setId(1L);
        chatBot.setActive(active);
        return chatBot;
    }
}
