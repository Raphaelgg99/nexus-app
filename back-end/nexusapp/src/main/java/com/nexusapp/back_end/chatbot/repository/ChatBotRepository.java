package com.nexusapp.back_end.chatbot.repository;

import com.nexusapp.back_end.chatbot.model.ChatBot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatBotRepository extends JpaRepository<ChatBot, Long> {
}
