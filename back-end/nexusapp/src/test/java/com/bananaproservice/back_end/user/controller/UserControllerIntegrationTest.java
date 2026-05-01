package com.nexusapp.back_end.user.controller;

import com.nexusapp.back_end.config.security.JwtAuthenticationFilter;
import com.nexusapp.back_end.config.security.RestAccessDeniedHandler;
import com.nexusapp.back_end.config.security.RestAuthenticationEntryPoint;
import com.nexusapp.back_end.config.security.SecurityConfig;
import com.nexusapp.back_end.folder.exception.ApiExceptionHandler;
import com.nexusapp.back_end.folder.exception.ResourceNotFoundException;
import com.nexusapp.back_end.user.dto.UserResponse;
import com.nexusapp.back_end.user.model.UserRole;
import com.nexusapp.back_end.user.service.ApplicationUserDetailsService;
import com.nexusapp.back_end.user.service.UserService;
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

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiExceptionHandler.class})
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

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
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messages[0]").value("Authentication is required to access this resource."));
    }

    @Test
    @WithMockUser(roles = "USER")
    void findAllShouldReturnForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.messages[0]").value("You do not have permission to access this resource."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findAllShouldReturnUsersForAdmin() throws Exception {
        when(userService.findAll()).thenReturn(List.of(
                new UserResponse(1L, "admin", UserRole.ADMIN),
                new UserResponse(2L, "banana", UserRole.USER)
        ));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userName").value("admin"))
                .andExpect(jsonPath("$[1].role").value("USER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createShouldReturnCreatedAndLocationHeader() throws Exception {
        when(userService.create(any())).thenReturn(new UserResponse(9L, "banana", UserRole.USER));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserPayload("banana", "USER", "123456"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/users/9"))
                .andExpect(jsonPath("$.userName").value("banana"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createShouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserPayload("", null, "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findByIdShouldReturnNotFoundWhenServiceThrows() throws Exception {
        when(userService.findById(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/v1/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("User not found with id: 99"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateShouldReturnOk() throws Exception {
        when(userService.update(any(), any())).thenReturn(new UserResponse(7L, "updated", UserRole.ADMIN));

        mockMvc.perform(put("/api/v1/users/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserPayload("updated", "ADMIN", "654321"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("updated"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteShouldReturnNoContent() throws Exception {
        doNothing().when(userService).delete(5L);

        mockMvc.perform(delete("/api/v1/users/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteShouldReturnNotFoundWhenServiceThrows() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 55"))
                .when(userService).delete(55L);

        mockMvc.perform(delete("/api/v1/users/55"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("User not found with id: 55"));
    }

    private record UserPayload(String userName, String role, String password) {
    }
}
