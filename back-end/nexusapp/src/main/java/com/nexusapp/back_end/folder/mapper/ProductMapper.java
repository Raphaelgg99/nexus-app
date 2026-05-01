package com.nexusapp.back_end.folder.mapper;

import com.nexusapp.back_end.folder.dto.ProductRequest;
import com.nexusapp.back_end.folder.dto.ProductResponse;
import com.nexusapp.back_end.folder.model.Folder;
import com.nexusapp.back_end.folder.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request, Folder folder) {
        return new Product(
                request.image().trim(),
                request.resolvedName(),
                request.trimmedDescription(),
                request.trimmedSku(),
                request.available(),
                request.stockQuantity(),
                folder
        );
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getImage(),
                product.getName(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.isAvailable(),
                product.getStockQuantity(),
                product.getFolder().getId()
        );
    }

    public void updateEntity(Product product, ProductRequest request) {
        product.setImage(request.image().trim());
        product.setName(request.resolvedName());
        product.setDescription(request.trimmedDescription());
        product.setSku(request.trimmedSku());
        product.setAvailable(request.available() != null && request.available());
        product.setStockQuantity(request.stockQuantity() != null ? request.stockQuantity() : 0);
    }
}
