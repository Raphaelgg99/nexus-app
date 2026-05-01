package com.nexusapp.back_end.chatbot.controller;

import com.nexusapp.back_end.chatbot.model.ChatBot;
import com.nexusapp.back_end.chatbot.service.ChatBotService;
import com.nexusapp.back_end.config.security.JwtAuthenticationFilter;
import com.nexusapp.back_end.config.security.RestAccessDeniedHandler;
import com.nexusapp.back_end.config.security.RestAuthenticationEntryPoint;
import com.nexusapp.back_end.config.security.SecurityConfig;
import com.nexusapp.back_end.user.service.ApplicationUserDetailsService;
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

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatBotController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class ChatBotControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatBotService chatBotService;

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
        }).when(jwtAuthenticationFilter).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getStatusShouldReturnUnauthorizedWhenThereIsNoAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/chatbot"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messages[0]").value("Authentication is required to access this resource."));
    }

    @Test
    @WithMockUser
    void getStatusShouldReturnConfigurationForAuthenticatedUser() throws Exception {
        when(chatBotService.getStatus()).thenReturn(chatBot(true));

        mockMvc.perform(get("/api/v1/chatbot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser
    void toggleShouldReturnUpdatedConfiguration() throws Exception {
        when(chatBotService.toggleStatus()).thenReturn(chatBot(false));

        mockMvc.perform(patch("/api/v1/chatbot/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser
    void getStatusShouldReturnInternalServerErrorWhenConfigurationDoesNotExist() throws Exception {
        when(chatBotService.getStatus()).thenThrow(new RuntimeException("Configuracao do ChatBot nao encontrada!"));

        mockMvc.perform(get("/api/v1/chatbot"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.messages[0]").value("Unexpected internal server error."));
    }

    private ChatBot chatBot(boolean active) {
        ChatBot chatBot = new ChatBot();
        chatBot.setId(1L);
        chatBot.setActive(active);
        return chatBot;
    }
}
