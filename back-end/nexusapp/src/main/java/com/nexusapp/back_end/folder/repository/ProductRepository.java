package com.nexusapp.back_end.folder.repository;

import com.nexusapp.back_end.folder.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByFolderIdOrderByIdAsc(Long folderId);
}
