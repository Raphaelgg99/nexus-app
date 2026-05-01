package com.nexusapp.back_end.folder.controller;

import com.nexusapp.back_end.config.security.JwtAuthenticationFilter;
import com.nexusapp.back_end.config.security.RestAccessDeniedHandler;
import com.nexusapp.back_end.config.security.RestAuthenticationEntryPoint;
import com.nexusapp.back_end.config.security.SecurityConfig;
import com.nexusapp.back_end.folder.dto.ProductResponse;
import com.nexusapp.back_end.folder.exception.ApiExceptionHandler;
import com.nexusapp.back_end.folder.exception.ResourceNotFoundException;
import com.nexusapp.back_end.folder.service.ProductService;
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

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, ApiExceptionHandler.class})
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

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
        mockMvc.perform(get("/api/v1/folders/1/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void findAllShouldReturnProductsForAuthenticatedUser() throws Exception {
        when(productService.findAllByFolderId(1L)).thenReturn(List.of(
                new ProductResponse(1L, "img-1", "mobile", "mobile", null, null, false, 0, 1L),
                new ProductResponse(2L, "img-2", "desktop", "desktop", null, null, true, 7, 1L)
        ));

        mockMvc.perform(get("/api/v1/folders/1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("mobile"))
                .andExpect(jsonPath("$[1].name").value("desktop"))
                .andExpect(jsonPath("$[1].folderId").value(1));
    }

    @Test
    @WithMockUser
    void createShouldReturnCreatedAndLocationHeader() throws Exception {
        when(productService.create(any(), any())).thenReturn(
                new ProductResponse(12L, "img", "mobile", "mobile", "desc", "sku-01", true, 5, 7L)
        );

        mockMvc.perform(post("/api/v1/folders/7/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductPayload("img", "mobile"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/folders/7/products/12"))
                .andExpect(jsonPath("$.folderId").value(7))
                .andExpect(jsonPath("$.name").value("mobile"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @WithMockUser
    void createShouldReturnBadRequestWhenProductPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/folders/7/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductPayload("", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").exists());
    }

    @Test
    @WithMockUser
    void legacyMockupsEndpointShouldRemainCompatible() throws Exception {
        when(productService.findById(2L, 8L))
                .thenReturn(new ProductResponse(8L, "img-8", "legacy", "legacy", null, null, true, 3, 2L));

        mockMvc.perform(get("/api/v1/folders/2/mockups/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("legacy"))
                .andExpect(jsonPath("$.folderId").value(2));
    }

    @Test
    @WithMockUser
    void findByIdShouldReturnNotFoundWhenServiceThrows() throws Exception {
        when(productService.findById(2L, 8L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 8 in folder id: 2"));

        mockMvc.perform(get("/api/v1/folders/2/products/8"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Product not found with id: 8 in folder id: 2"));
    }

    @Test
    @WithMockUser
    void updateShouldReturnOk() throws Exception {
        when(productService.update(any(), any(), any()))
                .thenReturn(new ProductResponse(3L, "new-image", "desktop", "desktop", null, null, false, 0, 1L));

        mockMvc.perform(put("/api/v1/folders/1/products/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductPayload("new-image", "desktop"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.image").value("new-image"))
                .andExpect(jsonPath("$.name").value("desktop"))
                .andExpect(jsonPath("$.type").value("desktop"));
    }

    @Test
    @WithMockUser
    void deleteShouldReturnNoContent() throws Exception {
        doNothing().when(productService).delete(1L, 5L);

        mockMvc.perform(delete("/api/v1/folders/1/products/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void deleteShouldReturnNotFoundWhenServiceThrows() throws Exception {
        doThrow(new ResourceNotFoundException("Product not found with id: 9"))
                .when(productService).delete(1L, 9L);

        mockMvc.perform(delete("/api/v1/folders/1/products/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Product not found with id: 9"));
    }

    private record ProductPayload(String image, String type) {
    }
}
