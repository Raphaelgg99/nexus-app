package com.nexusapp.back_end.folder.mapper;

import com.nexusapp.back_end.folder.dto.FolderRequest;
import com.nexusapp.back_end.folder.dto.FolderResponse;
import com.nexusapp.back_end.folder.model.Folder;
import org.springframework.stereotype.Component;

@Component
public class FolderMapper {

    public Folder toEntity(FolderRequest request) {
        return new Folder(request.name().trim());
    }

    public FolderResponse toResponse(Folder folder) {
        return new FolderResponse(folder.getId(), folder.getName());
    }

    public void updateEntity(Folder folder, FolderRequest request) {
        folder.setName(request.name().trim());
    }
}
