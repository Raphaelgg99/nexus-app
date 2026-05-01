package com.nexusapp.back_end.folder.dto;

public record ProductResponse(
        Long id,
        String image,
        String name,
        String type,
        String description,
        String sku,
        boolean available,
        int stockQuantity,
        Long folderId
) {
}
