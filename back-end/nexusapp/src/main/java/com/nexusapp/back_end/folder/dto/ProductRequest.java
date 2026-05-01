package com.nexusapp.back_end.folder.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank(message = "Image is required")
        String image,

        @Size(max = 80, message = "Name must have at most 80 characters")
        String name,

        @Size(max = 80, message = "Type must have at most 80 characters")
        String type,

        String description,

        @Size(max = 80, message = "SKU must have at most 80 characters")
        String sku,

        Boolean available,

        @PositiveOrZero(message = "Stock quantity must be zero or positive")
        Integer stockQuantity
) {

    @AssertTrue(message = "Name is required")
    public boolean hasNameOrType() {
        return hasText(name) || hasText(type);
    }

    public String resolvedName() {
        return hasText(name) ? name.trim() : type.trim();
    }

    public String trimmedDescription() {
        return hasText(description) ? description.trim() : null;
    }

    public String trimmedSku() {
        return hasText(sku) ? sku.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
