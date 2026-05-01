package com.nexusapp.back_end.bananapro.controller;

import com.nexusapp.back_end.bananapro.service.OpenAIService;
import com.nexusapp.back_end.config.security.JwtAuthenticationFilter;
import com.nexusapp.back_end.config.security.RestAccessDeniedHandler;
import com.nexusapp.back_end.config.security.RestAuthenticationEntryPoint;
import com.nexusapp.back_end.config.security.SecurityConfig;
import com.nexusapp.back_end.folder.exception.ApiExceptionHandler;
import com.nexusapp.back_end.user.service.ApplicationUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PromptController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiExceptionHandler.class})
class PromptControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OpenAIService openAIService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private ApplicationUserDetailsService applicationUserDetailsService;

    @BeforeEach
    void setUpFilter() throws Exception {
        doAnswer(invocation -> {
            ((FilterChain) invocation.getArgument(2)).doFilter(
                    (ServletRequest) invocation.getArgument(0),
                    (ServletResponse) invocation.getArgument(1)
            );
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void melhorarShouldReturnUnauthorizedWhenThereIsNoAuthentication() throws Exception {
        mockMvc.perform(post("/api/melhorar-prompt")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("prompt", "logo"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messages[0]").value("Authentication is required to access this resource."));
    }

    @Test
    @WithMockUser
    void melhorarShouldReturnImprovedPrompt() throws Exception {
        when(openAIService.melhorarPrompt("logo")).thenReturn(Mono.just("improved prompt"));

        MvcResult result = mockMvc.perform(post("/api/melhorar-prompt")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("prompt", "logo"))))
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt").value("improved prompt"));
    }

    @Test
    @WithMockUser
    void melhorarShouldReturnNotFoundWhenServiceReturnsEmpty() throws Exception {
        when(openAIService.melhorarPrompt("logo")).thenReturn(Mono.empty());

        MvcResult result = mockMvc.perform(post("/api/melhorar-prompt")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("prompt", "logo"))))
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void melhorarShouldReturnBadRequestWhenServiceRejectsPrompt() throws Exception {
        when(openAIService.melhorarPrompt("")).thenReturn(Mono.error(new IllegalArgumentException("Prompt invalido")));

        MvcResult result = mockMvc.perform(post("/api/melhorar-prompt")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("prompt", ""))))
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Prompt invalido"));
    }
}
