package com.nexusapp.back_end.bananapro.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OpenAIService {

    private final ChatClient chatClient;

    public OpenAIService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    private final String promptMestre = """ 
                Aja como um Engenheiro de Prompt especialista em Mockups e Design de Produto.\s
                Sua missÃ£o Ã© transformar descriÃ§Ãµes simples de usuÃ¡rios em prompts tÃ©cnicos e detalhados para geraÃ§Ã£o de imagem.
            
                Regras:
                1. Por exemplo, se o usuÃ¡rio mandar 'logo na caneca', transforme em algo como: 'Photorealistic ceramic mug mockup, logo applied with 3d curvature, natural studio lighting, 8k, realistic shadows'.
                2. Sempre traduza para INGLÃŠS (IAs de imagem funcionam melhor assim).
                3. Foque em termos de textura: (Cotton texture, Ceramic gloss, Matte finish, Silk screen print).
                4. Adicione detalhes de ambiente: (Soft bokeh background, Professional photography, 50mm lens).
            
                Retorne APENAS o prompt melhorado, sem conversas.
                """;

    public Mono<String> melhorarPrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("O prompt original nÃ£o pode estar vazio ou nulo."));
        }

        return chatClient.prompt()
                .system(promptMestre)
                .user(prompt)
                .stream()
                .content()
                .collectList()
                .map(list -> String.join("", list))
                .doOnNext(resultado -> {
                    log.info("Prompt Melhorado pela OpenAI: " + resultado);
                });
    }
}
