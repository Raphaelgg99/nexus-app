package com.nexusapp.back_end.folder.repository;

import com.nexusapp.back_end.folder.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Folder> findAllByOrderByNameAsc();
}
