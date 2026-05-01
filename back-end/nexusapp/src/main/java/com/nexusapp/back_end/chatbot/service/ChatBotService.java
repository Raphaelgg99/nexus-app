package com.nexusapp.back_end.chatbot.service;

import com.nexusapp.back_end.chatbot.model.ChatBot;
import com.nexusapp.back_end.chatbot.repository.ChatBotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatBotService {

    private final ChatBotRepository repository;

    public ChatBotService(ChatBotRepository repository) {
        this.repository = repository;
    }

    public ChatBot getStatus() {
        return repository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Configuração do ChatBot não encontrada!"));
    }

    @Transactional
    public ChatBot toggleStatus() {
        ChatBot config = getStatus();
        config.setActive(!config.isActive());
        return repository.save(config);
    }
}
