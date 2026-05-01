package com.nexusapp.back_end.folder.controller;

import com.nexusapp.back_end.folder.dto.ProductRequest;
import com.nexusapp.back_end.folder.dto.ProductResponse;
import com.nexusapp.back_end.folder.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/folders/{folderId}/products", "/api/v1/folders/{folderId}/mockups"})
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> findAll(@PathVariable Long folderId) {
        return ResponseEntity.ok(service.findAllByFolderId(folderId));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> findById(
            @PathVariable Long folderId,
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(service.findById(folderId, productId));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @PathVariable Long folderId,
            @Valid @RequestBody ProductRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        ProductResponse response = service.create(folderId, request);
        URI location = uriBuilder
                .path("/api/v1/folders/{folderId}/products/{productId}")
                .buildAndExpand(folderId, response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long folderId,
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(service.update(folderId, productId, request));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long folderId,
            @PathVariable Long productId
    ) {
        service.delete(folderId, productId);

        return ResponseEntity.noContent().build();
    }
}
