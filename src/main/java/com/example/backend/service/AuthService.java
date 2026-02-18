package com.example.backend.service;

import com.example.backend.domain.AppUser;
import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.exception.BadRequestException;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(
                user.getUsername(),
                user.getRole().name()
        );

        return new AuthResponse(token, user.getRole().name(), user.getUsername());
    }
}
