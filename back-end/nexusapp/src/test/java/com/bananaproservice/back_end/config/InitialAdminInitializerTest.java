package com.nexusapp.back_end.config;

import com.nexusapp.back_end.user.model.User;
import com.nexusapp.back_end.user.model.UserRole;
import com.nexusapp.back_end.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialAdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void runShouldCreateInitialAdminWhenThereAreNoUsersAndCredentialsExist() throws Exception {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        InitialAdminInitializer initializer = new InitialAdminInitializer(
                userRepository,
                passwordEncoder,
                "  admin  ",
                "123456"
        );

        initializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("admin", captor.getValue().getUserName());
        assertEquals(UserRole.ADMIN, captor.getValue().getRole());
        assertEquals("encoded-password", captor.getValue().getPassword());
    }

    @Test
    void runShouldDoNothingWhenUsersAlreadyExist() throws Exception {
        when(userRepository.count()).thenReturn(1L);
        InitialAdminInitializer initializer = new InitialAdminInitializer(
                userRepository,
                passwordEncoder,
                "admin",
                "123456"
        );

        initializer.run();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void runShouldDoNothingWhenCredentialsAreMissing() throws Exception {
        when(userRepository.count()).thenReturn(0L);
        InitialAdminInitializer initializer = new InitialAdminInitializer(
                userRepository,
                passwordEncoder,
                "",
                "123456"
        );

        initializer.run();

        verify(userRepository, never()).save(any(User.class));
    }
}
