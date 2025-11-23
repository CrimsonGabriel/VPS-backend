package com.bazunia.vps.controller;

import com.bazunia.vps.dto.AdminUserDto;
import com.bazunia.vps.dto.AdminUserRequest;
import com.bazunia.vps.model.Role;
import com.bazunia.vps.model.User;
import com.bazunia.vps.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        List<AdminUserDto> users = userRepository.findAll().stream()
                .map(AdminUserDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDto> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika o ID: " + id));
        return ResponseEntity.ok(AdminUserDto.fromEntity(user));
    }

    @PostMapping
    public ResponseEntity<AdminUserDto> createUser(@RequestBody AdminUserRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            // To złapie GlobalHandler jako 409 Conflict
            throw new IllegalStateException("Email jest już zajęty.");
        }
        if (request.password() == null || request.password().isEmpty()) {
            throw new IllegalArgumentException("Hasło jest wymagane.");
        }

        User newUser = new User();
        newUser.setEmail(request.email());
        newUser.setName(request.name());
        newUser.setRole(request.isAdmin() ? Role.ADMIN : Role.USER);
        newUser.setPassword(passwordEncoder.encode(request.password()));
        newUser.setTwoFactorEnabled(false);
        newUser.setEnabled(request.enabled());

        User saved = userRepository.save(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminUserDto.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUserDto> updateUser(@PathVariable Long id, @RequestBody AdminUserRequest request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika do edycji"));

        existingUser.setEmail(request.email());
        existingUser.setName(request.name());
        existingUser.setRole(request.isAdmin() ? Role.ADMIN : Role.USER);
        existingUser.setEnabled(request.enabled());

        if (request.password() != null && !request.password().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(request.password()));
        }

        User saved = userRepository.save(existingUser);
        return ResponseEntity.ok(AdminUserDto.fromEntity(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika do usunięcia"));

        if (userToDelete.getEmail().equals("admin")) {
            // Rzuci 403 w Handlerze
            throw new org.springframework.security.access.AccessDeniedException("Nie można usunąć głównego admina.");
        }

        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}