package com.example.backend.service;

import com.example.backend.domain.AppUser;
import com.example.backend.domain.Role;
import com.example.backend.exception.ConflictException;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AppUser createUser(String username, String password, Role role, String fullName) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ConflictException("Username already exists: " + username);
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setFullName(fullName);

        return userRepository.save(user);
    }
}
