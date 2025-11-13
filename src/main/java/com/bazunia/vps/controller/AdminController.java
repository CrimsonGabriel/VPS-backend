package com.bazunia.vps.controller;

import com.bazunia.vps.model.User;
import com.bazunia.vps.repository.UserRepository;
import com.bazunia.vps.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/users") // Ta ścieżka wymaga roli ADMIN
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // DTOs dla uproszczenia (możesz je przenieść do pakietu dto)
    public record AdminUserRequest(
            String email,
            String password, // Opcjonalne: jeśli podane, hasło zostanie zmienione
            String name,
            boolean isAdmin
    ) {}

    // --- READ (Wszyscy Użytkownicy) ---
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        // Zwraca listę wszystkich użytkowników (potrzebne do UsersPage.jsx)
        return ResponseEntity.ok(userRepository.findAll());
    }

    // --- READ (Pobierz po ID) ---
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- CREATE (Dodaj Użytkownika) ---
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody AdminUserRequest request) {
        // Walidacja czy użytkownik już istnieje
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Hasło jest wymagane przy tworzeniu
        if (request.password() == null || request.password().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        User newUser = new User();
        newUser.setEmail(request.email());
        newUser.setName(request.name());
        newUser.setRole(request.isAdmin() ? Role.ADMIN : Role.USER);
        newUser.setPassword(passwordEncoder.encode(request.password())); // Haszowanie hasła
        newUser.setTwoFactorEnabled(false); // Domyślnie 2FA wyłączone

        return ResponseEntity.status(HttpStatus.CREATED).body(userRepository.save(newUser));
    }

    // --- UPDATE (Edytuj Użytkownika) ---
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody AdminUserRequest request) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setEmail(request.email());
                    existingUser.setName(request.name());
                    existingUser.setRole(request.isAdmin() ? Role.ADMIN : Role.USER);

                    // Jeśli w formularzu podano nowe hasło, hashujemy i zmieniamy
                    if (request.password() != null && !request.password().isEmpty()) {
                        existingUser.setPassword(passwordEncoder.encode(request.password()));
                    }

                    return ResponseEntity.ok(userRepository.save(existingUser));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- DELETE (Usuń Użytkownika) ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        Optional<User> userToDelete = userRepository.findById(id);

        // Zabezpieczenie: Nie pozwól usunąć głównego admina (opcjonalnie)
        if (userToDelete.isPresent() && userToDelete.get().getEmail().equals("admin")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build(); // 204 No Content - sukces
        }
        return ResponseEntity.notFound().build();
    }
}