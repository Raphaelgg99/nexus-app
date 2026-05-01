package com.nexusapp.back_end.bananapro.controller;

import com.nexusapp.back_end.bananapro.model.NanoBanana;
import com.nexusapp.back_end.bananapro.service.ImageRenderingService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrototypeController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class PrototypeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageRenderingService imageRenderingService;

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
    void generateShouldReturnUnauthorizedWhenThereIsNoAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "mockup.png", "image/png", "content".getBytes());
        MockMultipartFile prompt = new MockMultipartFile("prompt", "", TEXT_PLAIN_VALUE, "landing page".getBytes());

        mockMvc.perform(multipart("/api/prototype/generate").file(file).file(prompt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void generateShouldReturnRenderedPrototype() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "mockup.png", "image/png", "content".getBytes());
        MockMultipartFile prompt = new MockMultipartFile("prompt", "", TEXT_PLAIN_VALUE, "landing page".getBytes());
        when(imageRenderingService.generatePrototype(any(), any()))
                .thenReturn(Mono.just(new NanoBanana("data:image/png;base64,abc123")));

        MvcResult result = mockMvc.perform(multipart("/api/prototype/generate").file(file).file(prompt))
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("data:image/png;base64,abc123"));
    }

    @Test
    @WithMockUser
    void generateShouldReturnInternalServerErrorWhenServiceFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "mockup.png", "image/png", "content".getBytes());
        MockMultipartFile prompt = new MockMultipartFile("prompt", "", TEXT_PLAIN_VALUE, "landing page".getBytes());
        when(imageRenderingService.generatePrototype(any(), any()))
                .thenReturn(Mono.error(new RuntimeException("Erro Gemini")));

        MvcResult result = mockMvc.perform(multipart("/api/prototype/generate").file(file).file(prompt))
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.messages[0]").value("Unexpected internal server error."));
    }
}
