package com.nexusapp.back_end.user.service;

import com.nexusapp.back_end.folder.exception.DuplicateResourceException;
import com.nexusapp.back_end.folder.exception.ResourceNotFoundException;
import com.nexusapp.back_end.user.dto.UserCreateRequest;
import com.nexusapp.back_end.user.dto.UserResponse;
import com.nexusapp.back_end.user.dto.UserUpdateRequest;
import com.nexusapp.back_end.user.mapper.UserMapper;
import com.nexusapp.back_end.user.model.User;
import com.nexusapp.back_end.user.model.UserRole;
import com.nexusapp.back_end.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, UserMapper mapper, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        logger.info("Fetching all users");
        return repository.findAllByOrderByUserNameAsc()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        logger.info("Fetching user by id: {}", id);
        return mapper.toResponse(findEntityById(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String normalizedUserName = request.userName().trim();
        logger.info("Creating user with username: {}", normalizedUserName);

        validateUserNameAvailability(normalizedUserName);

        UserRole roleToPersist = resolveRoleForCreation(request.role());
        User savedUser = repository.save(new User(
                normalizedUserName,
                roleToPersist,
                passwordEncoder.encode(request.password())
        ));

        logger.info("User created with id: {}", savedUser.getId());
        return mapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        String normalizedUserName = request.userName().trim();
        logger.info("Updating user with id: {}", id);

        User user = findEntityById(id);
        validateUserNameAvailability(normalizedUserName, id);
        validateAdminRoleChange(user, request.role());

        mapper.updateEntity(user, request);
        updatePasswordIfPresent(user, request.password());

        User updatedUser = repository.save(user);
        logger.info("User updated with id: {}", updatedUser.getId());
        return mapper.toResponse(updatedUser);
    }

    @Transactional
    public void delete(Long id) {
        logger.info("Deleting user with id: {}", id);
        User user = findEntityById(id);
        validateAdminRemoval(user);

        repository.delete(user);
        logger.info("User deleted with id: {}", id);
    }

    @Transactional(readOnly = true)
    public User findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public User findByUserName(String userName) {
        return repository.findByUserNameIgnoreCase(userName.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + userName));
    }

    private UserRole resolveRoleForCreation(UserRole requestedRole) {
        if (repository.count() == 0) {
            logger.info("Bootstrapping first user as ADMIN");
            return UserRole.ADMIN;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (!isAdmin) {
            throw new AccessDeniedException("Only administrators can create users after bootstrap.");
        }

        return requestedRole;
    }

    private void updatePasswordIfPresent(User user, String rawPassword) {
        if (rawPassword == null) {
            return;
        }

        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("Password must not be blank when provided.");
        }

        user.setPassword(passwordEncoder.encode(rawPassword));
    }

    private void validateUserNameAvailability(String userName) {
        if (repository.existsByUserNameIgnoreCase(userName)) {
            throw new DuplicateResourceException("Username already exists: " + userName);
        }
    }

    private void validateUserNameAvailability(String userName, Long id) {
        if (repository.existsByUserNameIgnoreCaseAndIdNot(userName, id)) {
            throw new DuplicateResourceException("Username already exists: " + userName);
        }
    }

    private void validateAdminRoleChange(User currentUser, UserRole requestedRole) {
        if (currentUser.getRole() == UserRole.ADMIN
                && requestedRole != UserRole.ADMIN
                && repository.countByRole(UserRole.ADMIN) <= 1) {
            throw new IllegalStateException("The last administrator cannot be demoted.");
        }
    }

    private void validateAdminRemoval(User user) {
        if (user.getRole() == UserRole.ADMIN && repository.countByRole(UserRole.ADMIN) <= 1) {
            throw new IllegalStateException("The last administrator cannot be removed.");
        }
    }
}
