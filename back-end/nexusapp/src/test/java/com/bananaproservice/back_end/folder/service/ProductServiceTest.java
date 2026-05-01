package com.nexusapp.back_end.folder.service;

import com.nexusapp.back_end.folder.dto.ProductRequest;
import com.nexusapp.back_end.folder.dto.ProductResponse;
import com.nexusapp.back_end.folder.exception.ResourceNotFoundException;
import com.nexusapp.back_end.folder.mapper.ProductMapper;
import com.nexusapp.back_end.folder.model.Folder;
import com.nexusapp.back_end.folder.model.Product;
import com.nexusapp.back_end.folder.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private FolderService folderService;

    @Test
    void findAllByFolderIdShouldValidateFolderAndMapResponses() {
        ProductService service = new ProductService(repository, new ProductMapper(), folderService);
        Folder folder = folderWithId(1L, "Folder");
        when(folderService.findEntityById(1L)).thenReturn(folder);
        when(repository.findAllByFolderIdOrderByIdAsc(1L)).thenReturn(List.of(
                productWithId(10L, "img-1", "mobile", folder),
                productWithId(11L, "img-2", "desktop", folder)
        ));

        List<ProductResponse> response = service.findAllByFolderId(1L);

        assertEquals(2, response.size());
        assertEquals(1L, response.get(0).folderId());
        assertEquals("desktop", response.get(1).type());
        assertEquals("desktop", response.get(1).name());
    }

    @Test
    void createShouldUseFolderFromFolderServiceAndTrimFields() {
        ProductService service = new ProductService(repository, new ProductMapper(), folderService);
        Folder folder = folderWithId(2L, "Folder");
        when(folderService.findEntityById(2L)).thenReturn(folder);
        when(repository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            Product savedProduct = new Product(
                    product.getImage(),
                    product.getName(),
                    product.getDescription(),
                    product.getSku(),
                    product.isAvailable(),
                    product.getStockQuantity(),
                    product.getFolder()
            );
            setProductId(savedProduct, 30L);
            return savedProduct;
        });

        ProductResponse response = service.create(
                2L,
                new ProductRequest("  image-data  ", null, "  mobile  ", "  desc  ", "  sku-01  ", true, 12)
        );

        assertEquals(30L, response.id());
        assertEquals("image-data", response.image());
        assertEquals("mobile", response.type());
        assertEquals("mobile", response.name());
        assertEquals("desc", response.description());
        assertEquals("sku-01", response.sku());
        assertEquals(12, response.stockQuantity());
        assertEquals(true, response.available());
        assertEquals(2L, response.folderId());
    }

    @Test
    void updateShouldThrowWhenProductBelongsToAnotherFolder() {
        ProductService service = new ProductService(repository, new ProductMapper(), folderService);
        Folder requestedFolder = folderWithId(5L, "Requested");
        Folder otherFolder = folderWithId(6L, "Other");
        when(folderService.findEntityById(5L)).thenReturn(requestedFolder);
        when(repository.findById(40L)).thenReturn(Optional.of(productWithId(40L, "img", "web", otherFolder)));

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.update(5L, 40L, new ProductRequest("img-2", null, "mobile", null, null, null, null))
        );
    }

    @Test
    void deleteShouldRemoveExistingProduct() {
        ProductService service = new ProductService(repository, new ProductMapper(), folderService);
        Folder folder = folderWithId(4L, "Folder");
        Product product = productWithId(20L, "img", "mobile", folder);
        when(folderService.findEntityById(4L)).thenReturn(folder);
        when(repository.findById(20L)).thenReturn(Optional.of(product));

        service.delete(4L, 20L);

        verify(repository).delete(product);
    }

    @Test
    void findByIdShouldThrowWhenProductDoesNotExist() {
        ProductService service = new ProductService(repository, new ProductMapper(), folderService);
        Folder folder = folderWithId(1L, "Folder");
        when(folderService.findEntityById(1L)).thenReturn(folder);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(1L, 99L));
    }

    @Test
    void updateShouldPreserveImageAndRefreshProductFields() {
        ProductService service = new ProductService(repository, new ProductMapper(), folderService);
        Folder folder = folderWithId(7L, "Folder");
        Product product = productWithId(33L, "old-image", "old-name", folder);
        when(folderService.findEntityById(7L)).thenReturn(folder);
        when(repository.findById(33L)).thenReturn(Optional.of(product));
        when(repository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = service.update(
                7L,
                33L,
                new ProductRequest("new-image", "  new-name  ", null, "  desc  ", "  sku-99  ", true, 4)
        );

        assertEquals(33L, response.id());
        assertEquals("new-image", response.image());
        assertEquals("new-name", response.name());
        assertEquals("new-name", response.type());
        assertEquals("desc", response.description());
        assertEquals("sku-99", response.sku());
        assertEquals(true, response.available());
        assertEquals(4, response.stockQuantity());
    }

    @Test
    void findAllByFolderIdShouldHandleLegacyRowsWithNullProductFlags() {
        ProductService service = new ProductService(repository, new ProductMapper(), folderService);
        Folder folder = folderWithId(9L, "Folder");
        Product legacyProduct = productWithId(50L, "img-legacy", "legacy", folder);
        legacyProduct.setAvailable(null);
        legacyProduct.setStockQuantity(null);
        when(folderService.findEntityById(9L)).thenReturn(folder);
        when(repository.findAllByFolderIdOrderByIdAsc(9L)).thenReturn(List.of(legacyProduct));

        List<ProductResponse> response = service.findAllByFolderId(9L);

        assertEquals(1, response.size());
        assertEquals(false, response.get(0).available());
        assertEquals(0, response.get(0).stockQuantity());
    }

    private Folder folderWithId(Long id, String name) {
        Folder folder = new Folder(name);
        try {
            var field = Folder.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(folder, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        return folder;
    }

    private Product productWithId(Long id, String image, String name, Folder folder) {
        Product product = new Product(image, name, null, null, false, 0, folder);
        setProductId(product, id);
        return product;
    }

    private void setProductId(Product product, Long id) {
        try {
            var field = Product.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(product, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
