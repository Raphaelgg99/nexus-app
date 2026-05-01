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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, new UserMapper(), passwordEncoder);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAllShouldMapRepositoryResults() {
        User admin = userWithId(1L, "admin", UserRole.ADMIN, "encoded-admin");
        User user = userWithId(2L, "banana", UserRole.USER, "encoded-user");
        when(repository.findAllByOrderByUserNameAsc()).thenReturn(List.of(admin, user));

        List<UserResponse> result = service.findAll();

        assertEquals(2, result.size());
        assertEquals("admin", result.get(0).userName());
        assertEquals(UserRole.USER, result.get(1).role());
    }

    @Test
    void createShouldBootstrapFirstUserAsAdmin() {
        UserCreateRequest request = new UserCreateRequest("  first-admin  ", UserRole.USER, "123456");
        when(repository.count()).thenReturn(0L);
        when(repository.existsByUserNameIgnoreCase("first-admin")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            return userWithId(10L, saved.getUserName(), saved.getRole(), saved.getPassword());
        });

        UserResponse response = service.create(request);

        assertEquals(10L, response.id());
        assertEquals("first-admin", response.userName());
        assertEquals(UserRole.ADMIN, response.role());
    }

    @Test
    void createShouldRejectNonAdminAfterBootstrap() {
        UserCreateRequest request = new UserCreateRequest("new-user", UserRole.USER, "123456");
        when(repository.count()).thenReturn(1L);
        when(repository.existsByUserNameIgnoreCase("new-user")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.create(request));
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void createShouldAllowAdminAfterBootstrap() {
        UserCreateRequest request = new UserCreateRequest("new-user", UserRole.USER, "123456");
        when(repository.count()).thenReturn(2L);
        when(repository.existsByUserNameIgnoreCase("new-user")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            return userWithId(11L, saved.getUserName(), saved.getRole(), saved.getPassword());
        });
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        UserResponse response = service.create(request);

        assertEquals(UserRole.USER, response.role());
        assertEquals("new-user", response.userName());
    }

    @Test
    void createShouldRejectDuplicateUserName() {
        UserCreateRequest request = new UserCreateRequest("admin", UserRole.ADMIN, "123456");
        when(repository.existsByUserNameIgnoreCase("admin")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.create(request));
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void updateShouldChangeFieldsAndEncodePasswordWhenProvided() {
        User existing = userWithId(7L, "old-user", UserRole.USER, "old-password");
        UserUpdateRequest request = new UserUpdateRequest("  new-user  ", UserRole.ADMIN, "654321");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.existsByUserNameIgnoreCaseAndIdNot("new-user", 7L)).thenReturn(false);
        when(passwordEncoder.encode("654321")).thenReturn("encoded-password");
        when(repository.save(existing)).thenReturn(existing);

        UserResponse response = service.update(7L, request);

        assertEquals("new-user", existing.getUserName());
        assertEquals(UserRole.ADMIN, existing.getRole());
        assertEquals("encoded-password", existing.getPassword());
        assertEquals("new-user", response.userName());
    }

    @Test
    void updateShouldRejectBlankPasswordWhenProvided() {
        User existing = userWithId(7L, "old-user", UserRole.USER, "old-password");
        UserUpdateRequest request = new UserUpdateRequest("new-user", UserRole.USER, "   ");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.existsByUserNameIgnoreCaseAndIdNot("new-user", 7L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.update(7L, request)
        );

        assertEquals("Password must not be blank when provided.", exception.getMessage());
    }

    @Test
    void updateShouldRejectDemotingLastAdmin() {
        User existing = userWithId(1L, "admin", UserRole.ADMIN, "encoded");
        UserUpdateRequest request = new UserUpdateRequest("admin", UserRole.USER, null);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByUserNameIgnoreCaseAndIdNot("admin", 1L)).thenReturn(false);
        when(repository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.update(1L, request)
        );

        assertEquals("The last administrator cannot be demoted.", exception.getMessage());
    }

    @Test
    void deleteShouldRejectRemovingLastAdmin() {
        User existing = userWithId(1L, "admin", UserRole.ADMIN, "encoded");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.delete(1L));

        assertEquals("The last administrator cannot be removed.", exception.getMessage());
    }

    @Test
    void findByUserNameShouldTrimInput() {
        User existing = userWithId(3L, "banana", UserRole.USER, "encoded");
        when(repository.findByUserNameIgnoreCase("banana")).thenReturn(Optional.of(existing));

        User result = service.findByUserName("  banana  ");

        assertNotNull(result);
        assertEquals("banana", result.getUserName());
    }

    @Test
    void findByIdShouldThrowWhenUserDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.findById(99L)
        );

        assertTrue(exception.getMessage().contains("99"));
    }

    private User userWithId(Long id, String userName, UserRole role, String password) {
        User user = new User(userName, role, password);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        return user;
    }
}
