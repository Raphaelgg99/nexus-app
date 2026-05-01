package com.nexusapp.back_end.user.service;

import com.nexusapp.back_end.config.security.JwtService;
import com.nexusapp.back_end.user.dto.AuthRequest;
import com.nexusapp.back_end.user.dto.AuthResponse;
import com.nexusapp.back_end.user.mapper.UserMapper;
import com.nexusapp.back_end.user.model.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserService userService,
            UserMapper userMapper,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
    }

    public AuthResponse login(AuthRequest request) {
        String normalizedUserName = request.userName().trim();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedUserName, request.password())
        );

        User user = userService.findByUserName(normalizedUserName);

        return new AuthResponse(
                jwtService.getTokenPrefix(),
                jwtService.generateToken(user),
                jwtService.getExpirationMillis(),
                userMapper.toResponse(user)
        );
    }
}
