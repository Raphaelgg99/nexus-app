package com.nexusapp.back_end.folder.service;

import com.nexusapp.back_end.folder.dto.FolderRequest;
import com.nexusapp.back_end.folder.dto.FolderResponse;
import com.nexusapp.back_end.folder.exception.DuplicateResourceException;
import com.nexusapp.back_end.folder.exception.ResourceNotFoundException;
import com.nexusapp.back_end.folder.mapper.FolderMapper;
import com.nexusapp.back_end.folder.model.Folder;
import com.nexusapp.back_end.folder.repository.FolderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

    @Mock
    private FolderRepository repository;

    @Test
    void findAllShouldReturnFoldersOrderedFromRepository() {
        FolderService service = new FolderService(repository, new FolderMapper());
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(
                folderWithId(1L, "A"),
                folderWithId(2L, "B")
        ));

        List<FolderResponse> response = service.findAll();

        assertEquals(2, response.size());
        assertEquals("A", response.get(0).name());
        assertEquals("B", response.get(1).name());
    }

    @Test
    void createShouldTrimNameAndPersistFolder() {
        FolderService service = new FolderService(repository, new FolderMapper());
        when(repository.existsByNameIgnoreCase("Landing Page")).thenReturn(false);
        when(repository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            return folderWithId(8L, folder.getName());
        });

        FolderResponse response = service.create(new FolderRequest("  Landing Page  "));

        assertEquals(8L, response.id());
        assertEquals("Landing Page", response.name());
    }

    @Test
    void createShouldRejectDuplicateFolderName() {
        FolderService service = new FolderService(repository, new FolderMapper());
        when(repository.existsByNameIgnoreCase("Landing Page")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.create(new FolderRequest("Landing Page")));
        verify(repository, never()).save(any(Folder.class));
    }

    @Test
    void updateShouldChangeFolderName() {
        FolderService service = new FolderService(repository, new FolderMapper());
        Folder existing = folderWithId(3L, "Old");
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameIgnoreCaseAndIdNot("New Folder", 3L)).thenReturn(false);
        when(repository.save(existing)).thenReturn(existing);

        FolderResponse response = service.update(3L, new FolderRequest("  New Folder  "));

        assertEquals("New Folder", existing.getName());
        assertEquals("New Folder", response.name());
    }

    @Test
    void updateShouldRejectDuplicateFolderName() {
        FolderService service = new FolderService(repository, new FolderMapper());
        Folder existing = folderWithId(3L, "Old");
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameIgnoreCaseAndIdNot("New Folder", 3L)).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> service.update(3L, new FolderRequest("New Folder"))
        );
    }

    @Test
    void deleteShouldRemoveExistingFolder() {
        FolderService service = new FolderService(repository, new FolderMapper());
        Folder existing = folderWithId(5L, "Folder");
        when(repository.findById(5L)).thenReturn(Optional.of(existing));

        service.delete(5L);

        verify(repository).delete(existing);
    }

    @Test
    void findByIdShouldThrowWhenFolderDoesNotExist() {
        FolderService service = new FolderService(repository, new FolderMapper());
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
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
}
