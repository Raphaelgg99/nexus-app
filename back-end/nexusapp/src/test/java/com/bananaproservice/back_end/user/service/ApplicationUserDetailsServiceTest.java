package com.nexusapp.back_end.user.service;

import com.nexusapp.back_end.user.model.User;
import com.nexusapp.back_end.user.model.UserRole;
import com.nexusapp.back_end.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationUserDetailsServiceTest {

    @Mock
    private UserRepository repository;

    @Test
    void loadUserByUsernameShouldTrimInputAndReturnSpringUserDetails() {
        ApplicationUserDetailsService service = new ApplicationUserDetailsService(repository);
        when(repository.findByUserNameIgnoreCase("admin"))
                .thenReturn(Optional.of(new User("admin", UserRole.ADMIN, "encoded")));

        UserDetails result = service.loadUserByUsername("  admin  ");

        assertEquals("admin", result.getUsername());
        assertEquals("encoded", result.getPassword());
        assertEquals("ROLE_ADMIN", result.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadUserByUsernameShouldThrowWhenUserDoesNotExist() {
        ApplicationUserDetailsService service = new ApplicationUserDetailsService(repository);
        when(repository.findByUserNameIgnoreCase("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ghost"));
    }
}
