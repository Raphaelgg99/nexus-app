package com.nexusapp.back_end.folder.service;

import com.nexusapp.back_end.folder.dto.ProductRequest;
import com.nexusapp.back_end.folder.dto.ProductResponse;
import com.nexusapp.back_end.folder.exception.ResourceNotFoundException;
import com.nexusapp.back_end.folder.mapper.ProductMapper;
import com.nexusapp.back_end.folder.model.Folder;
import com.nexusapp.back_end.folder.model.Product;
import com.nexusapp.back_end.folder.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repository;
    private final ProductMapper mapper;
    private final FolderService folderService;

    public ProductService(ProductRepository repository, ProductMapper mapper, FolderService folderService) {
        this.repository = repository;
        this.mapper = mapper;
        this.folderService = folderService;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAllByFolderId(Long folderId) {
        logger.info("Fetching all products for folder id: {}", folderId);
        folderService.findEntityById(folderId);

        return repository.findAllByFolderIdOrderByIdAsc(folderId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long folderId, Long id) {
        logger.info("Fetching product by id: {} for folder id: {}", id, folderId);

        return mapper.toResponse(findEntityById(folderId, id));
    }

    @Transactional
    public ProductResponse create(Long folderId, ProductRequest request) {
        logger.info("Creating product in folder id: {} with name: {}", folderId, request.resolvedName());

        Folder folder = folderService.findEntityById(folderId);
        Product product = mapper.toEntity(request, folder);
        Product savedProduct = repository.save(product);

        logger.info("Product created with id: {}", savedProduct.getId());
        return mapper.toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse update(Long folderId, Long id, ProductRequest request) {
        logger.info("Updating product with id: {} for folder id: {}", id, folderId);

        Product product = findEntityById(folderId, id);
        mapper.updateEntity(product, request);
        Product updatedProduct = repository.save(product);

        logger.info("Product updated with id: {}", updatedProduct.getId());
        return mapper.toResponse(updatedProduct);
    }

    @Transactional
    public void delete(Long folderId, Long id) {
        logger.info("Deleting product with id: {} for folder id: {}", id, folderId);

        Product product = findEntityById(folderId, id);
        repository.delete(product);

        logger.info("Product deleted with id: {}", id);
    }

    private Product findEntityById(Long folderId, Long id) {
        folderService.findEntityById(folderId);

        Product product = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (!product.getFolder().getId().equals(folderId)) {
            throw new ResourceNotFoundException(
                    "Product not found with id: " + id + " in folder id: " + folderId
            );
        }

        return product;
    }
}
