package com.nexusapp.back_end.bananapro.service;

import com.nexusapp.back_end.bananapro.model.NanoBanana;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageRenderingService {

    private final WebClient.Builder webClientBuilder;

    @Value("${api.nanobanana.key}")
    private String apiKey;

    public Mono<NanoBanana> generatePrototype(List<MultipartFile> files, String prompt) {
        log.info("Requisicao recebida gerando imagem...");
        log.info("Prompt: {}", prompt);

        List<Map<String, Object>> parts = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                String mimeType = resolveMimeType(file);
                String base64 = Base64.getEncoder().encodeToString(file.getBytes());

                parts.add(Map.of(
                        "inline_data", Map.of(
                                "mime_type", mimeType,
                                "data", base64
                        )
                ));
            }
        } catch (IOException exception) {
            return Mono.error(new IllegalStateException("Nao foi possivel ler as imagens enviadas.", exception));
        }

        parts.add(Map.of("text", prompt));

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", parts)),
                "generationConfig", Map.of("responseModalities", List.of("Text", "Image"))
        );

        return webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build()
                .post()
                .uri("/v1beta/models/gemini-2.5-flash-image:generateContent?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new RuntimeException("Erro Gemini: " + error)))
                )
                .bodyToMono(JsonNode.class)
                .map(json -> {
                    JsonNode partsNode = json.at("/candidates/0/content/parts");

                    for (JsonNode part : partsNode) {
                        if (part.has("inlineData")) {
                            String imageUrl = "data:image/png;base64," +
                                    part.at("/inlineData/data").asText();
                            return new NanoBanana(imageUrl);
                        }
                    }

                    throw new RuntimeException("IA nao retornou o prototipo renderizado");
                });
    }

    private String resolveMimeType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            return MediaType.IMAGE_PNG_VALUE;
        }

        return contentType;
    }
}
