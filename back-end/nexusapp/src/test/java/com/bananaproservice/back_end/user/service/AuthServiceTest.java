package com.nexusapp.back_end.user.service;

import com.nexusapp.back_end.config.security.JwtService;
import com.nexusapp.back_end.user.dto.AuthRequest;
import com.nexusapp.back_end.user.dto.AuthResponse;
import com.nexusapp.back_end.user.dto.UserResponse;
import com.nexusapp.back_end.user.mapper.UserMapper;
import com.nexusapp.back_end.user.model.User;
import com.nexusapp.back_end.user.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Test
    void loginShouldAuthenticateAndBuildResponseWithNormalizedUserName() {
        AuthService authService = new AuthService(authenticationManager, userService, new UserMapper(), jwtService);
        AuthRequest request = new AuthRequest("  admin  ", "123456");
        User user = new User("admin", UserRole.ADMIN, "encoded");
        when(userService.findByUserName("admin")).thenReturn(user);
        when(jwtService.getTokenPrefix()).thenReturn("Bearer");
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationMillis()).thenReturn(3600_000L);

        AuthResponse response = authService.login(request);

        verify(authenticationManager).authenticate(argThat(authentication ->
                authentication instanceof UsernamePasswordAuthenticationToken
                        && "admin".equals(authentication.getPrincipal())
                        && "123456".equals(authentication.getCredentials())
        ));
        verify(userService).findByUserName("admin");
        assertEquals("Bearer", response.tokenType());
        assertEquals("jwt-token", response.accessToken());
        assertEquals(3600_000L, response.expiresIn());
        assertEquals(new UserResponse(null, "admin", UserRole.ADMIN), response.user());
    }
}
