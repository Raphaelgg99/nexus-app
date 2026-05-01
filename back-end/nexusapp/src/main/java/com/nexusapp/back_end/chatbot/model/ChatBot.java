package com.nexusapp.back_end.chatbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_chatbot")
@Getter
@Setter
public class ChatBot {

    @Id
    private Long id;

    @Column(nullable = false)
    private boolean isActive;

}
