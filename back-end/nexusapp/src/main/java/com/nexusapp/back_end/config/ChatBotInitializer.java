package com.nexusapp.back_end.config;

import com.nexusapp.back_end.chatbot.model.ChatBot;
import com.nexusapp.back_end.chatbot.repository.ChatBotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ChatBotInitializer implements CommandLineRunner {

    @Autowired
    private ChatBotRepository repository;

    @Override
    public void run(String... args) {
        if (repository.findById(1L).isEmpty()) {
            ChatBot chatBot = new ChatBot();
            chatBot.setId(1L);
            chatBot.setActive(true);
            repository.save(chatBot);
            System.out.println("âœ… ConfiguraÃ§Ã£o inicial do ChatBot criada no banco!");
        }
    }
}