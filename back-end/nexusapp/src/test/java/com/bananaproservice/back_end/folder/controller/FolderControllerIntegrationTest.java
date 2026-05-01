package com.nexusapp.back_end.folder.controller;

import com.nexusapp.back_end.config.security.JwtAuthenticationFilter;
import com.nexusapp.back_end.config.security.RestAccessDeniedHandler;
import com.nexusapp.back_end.config.security.RestAuthenticationEntryPoint;
import com.nexusapp.back_end.config.security.SecurityConfig;
import com.nexusapp.back_end.folder.dto.FolderResponse;
import com.nexusapp.back_end.folder.exception.ApiExceptionHandler;
import com.nexusapp.back_end.folder.exception.ResourceNotFoundException;
import com.nexusapp.back_end.folder.service.FolderService;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FolderController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiExceptionHandler.class})
class FolderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FolderService folderService;

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
    void findAllShouldReturnUnauthorizedWhenThereIsNoAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/folders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messages[0]").value("Authentication is required to access this resource."));
    }

    @Test
    @WithMockUser
    void findAllShouldReturnFoldersForAuthenticatedUser() throws Exception {
        when(folderService.findAll()).thenReturn(List.of(
                new FolderResponse(1L, "Branding"),
                new FolderResponse(2L, "Website")
        ));

        mockMvc.perform(get("/api/v1/folders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Branding"))
                .andExpect(jsonPath("$[1].name").value("Website"));
    }

    @Test
    @WithMockUser
    void createShouldReturnCreatedAndLocationHeader() throws Exception {
        when(folderService.create(any())).thenReturn(new FolderResponse(9L, "New Folder"));

        mockMvc.perform(post("/api/v1/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FolderPayload("New Folder"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/folders/9"))
                .andExpect(jsonPath("$.name").value("New Folder"));
    }

    @Test
    @WithMockUser
    void createShouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FolderPayload(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").exists());
    }

    @Test
    @WithMockUser
    void findByIdShouldReturnNotFoundWhenServiceThrows() throws Exception {
        when(folderService.findById(77L)).thenThrow(new ResourceNotFoundException("Folder not found with id: 77"));

        mockMvc.perform(get("/api/v1/folders/77"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Folder not found with id: 77"));
    }

    @Test
    @WithMockUser
    void updateShouldReturnOk() throws Exception {
        when(folderService.update(any(), any())).thenReturn(new FolderResponse(4L, "Updated"));

        mockMvc.perform(put("/api/v1/folders/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FolderPayload("Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    @WithMockUser
    void deleteShouldReturnNoContent() throws Exception {
        doNothing().when(folderService).delete(5L);

        mockMvc.perform(delete("/api/v1/folders/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void deleteShouldReturnNotFoundWhenServiceThrows() throws Exception {
        doThrow(new ResourceNotFoundException("Folder not found with id: 55"))
                .when(folderService).delete(55L);

        mockMvc.perform(delete("/api/v1/folders/55"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Folder not found with id: 55"));
    }

    private record FolderPayload(String name) {
    }
}
