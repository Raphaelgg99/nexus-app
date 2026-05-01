package com.nexusapp.back_end.user.mapper;

import com.nexusapp.back_end.user.dto.UserResponse;
import com.nexusapp.back_end.user.dto.UserUpdateRequest;
import com.nexusapp.back_end.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUserName(), user.getRole());
    }

    public void updateEntity(User user, UserUpdateRequest request) {
        user.setUserName(request.userName().trim());
        user.setRole(request.role());
    }
}
