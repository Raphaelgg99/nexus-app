package com.nexusapp.back_end.folder.service;

import com.nexusapp.back_end.folder.dto.FolderRequest;
import com.nexusapp.back_end.folder.dto.FolderResponse;
import com.nexusapp.back_end.folder.exception.DuplicateResourceException;
import com.nexusapp.back_end.folder.exception.ResourceNotFoundException;
import com.nexusapp.back_end.folder.mapper.FolderMapper;
import com.nexusapp.back_end.folder.model.Folder;
import com.nexusapp.back_end.folder.repository.FolderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FolderService {

    private static final Logger logger = LoggerFactory.getLogger(FolderService.class);

    private final FolderRepository repository;
    private final FolderMapper mapper;

    public FolderService(FolderRepository repository, FolderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> findAll() {
        logger.info("Fetching all folders");

        return repository.findAllByOrderByNameAsc()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FolderResponse findById(Long id) {
        logger.info("Fetching folder by id: {}", id);

        return mapper.toResponse(findEntityById(id));
    }

    @Transactional
    public FolderResponse create(FolderRequest request) {
        String normalizedName = request.name().trim();
        logger.info("Creating folder with name: {}", normalizedName);

        validateFolderNameAvailability(normalizedName);

        Folder savedFolder = repository.save(new Folder(normalizedName));

        logger.info("Folder created with id: {}", savedFolder.getId());
        return mapper.toResponse(savedFolder);
    }

    @Transactional
    public FolderResponse update(Long id, FolderRequest request) {
        String normalizedName = request.name().trim();
        logger.info("Updating folder with id: {}", id);

        Folder folder = findEntityById(id);
        validateFolderNameAvailability(normalizedName, id);
        mapper.updateEntity(folder, request);

        Folder updatedFolder = repository.save(folder);
        logger.info("Folder updated with id: {}", updatedFolder.getId());

        return mapper.toResponse(updatedFolder);
    }

    @Transactional
    public void delete(Long id) {
        logger.info("Deleting folder with id: {} and all related mockups", id);

        Folder folder = findEntityById(id);
        repository.delete(folder);

        logger.info("Folder deleted with id: {}", id);
    }

    @Transactional(readOnly = true)
    public Folder findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found with id: " + id));
    }

    private void validateFolderNameAvailability(String name) {
        if (repository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Folder name already exists: " + name);
        }
    }

    private void validateFolderNameAvailability(String name, Long id) {
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Folder name already exists: " + name);
        }
    }
}
