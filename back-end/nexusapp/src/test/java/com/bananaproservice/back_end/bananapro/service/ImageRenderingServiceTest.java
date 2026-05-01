package com.nexusapp.back_end.bananapro.service;

import com.nexusapp.back_end.bananapro.model.NanoBanana;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ImageRenderingServiceTest {

    @Test
    void generatePrototypeShouldReturnBase64ImageFromApiResponse() {
        ExchangeFunction exchangeFunction = request -> {
            assertNotNull(request.headers().getFirst(HttpHeaders.CONTENT_TYPE));
            String json = """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [
                              {
                                "inlineData": {
                                  "data": "abc123"
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                    """;

            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .build());
        };
        ImageRenderingService service = new ImageRenderingService(WebClient.builder().exchangeFunction(exchangeFunction));
        ReflectionTestUtils.setField(service, "apiKey", "test-key");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "mockup.png",
                MediaType.IMAGE_PNG_VALUE,
                "file-content".getBytes(StandardCharsets.UTF_8)
        );

        StepVerifier.create(service.generatePrototype(List.of(file), "landing page"))
                .expectNextMatches(response -> "data:image/png;base64,abc123".equals(response.getImageUrl()))
                .verifyComplete();
    }

    @Test
    void generatePrototypeShouldFailWhenApiDoesNotReturnRenderedImage() {
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"no image\"}]}}]}")
                .build());
        ImageRenderingService service = new ImageRenderingService(WebClient.builder().exchangeFunction(exchangeFunction));
        ReflectionTestUtils.setField(service, "apiKey", "test-key");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "mockup.png",
                MediaType.IMAGE_PNG_VALUE,
                "file-content".getBytes(StandardCharsets.UTF_8)
        );

        StepVerifier.create(service.generatePrototype(List.of(file), "landing page"))
                .expectErrorMatches(error -> error instanceof RuntimeException
                        && error.getMessage().contains("IA nao retornou"))
                .verify();
    }
}
