package com.bazunia.vps.controller;

import com.bazunia.vps.model.User;
import com.bazunia.vps.repository.UserRepository;
import com.bazunia.vps.model.Role;
import com.bazunia.vps.service.UpdateService; // <-- NOWY IMPORT
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Można też użyć @RequestAttribute
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.bazunia.vps.dto.PasswordRequest;
import com.bazunia.vps.service.DataService;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.List;
import java.util.Map; // <-- NOWY IMPORT
import java.util.Optional;

@RestController
// Zmieniamy ścieżkę bazową na /api/admin, aby kontroler obsługiwał też inne sekcje admina
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UpdateService updateService;
    private final DataService dataService;

    // DTOs dla uproszczenia (możesz je przenieść do pakietu dto)
    public record AdminUserRequest(
            String email,
            String password, // Opcjonalne: jeśli podane, hasło zostanie zmienione
            String name,
            boolean isAdmin
    ) {}

    // DTOs dla aktualizacji
    public record UpdateStatusRequest(
            String key,     // "App", "GW-01", "Sensor-X"
            String status,  // "REQUIRED", "OPTIONAL", "NONE"
            boolean shouldNotify // Czy Android ma wyświetlić powiadomienie
    ) {}

    // ----------------------------------------------------------------------
    // --- 1. ZARZĄDZANIE UŻYTKOWNIKAMI (/api/admin/users/...) ---
    // ----------------------------------------------------------------------

    // --- READ (Wszyscy Użytkownicy) ---
    @GetMapping("/users") // <-- DODANY /users
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // --- READ (Pobierz po ID) ---
    @GetMapping("/users/{id}") // <-- DODANY /users
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- CREATE (Dodaj Użytkownika) ---
    @PostMapping("/users") // <-- DODANY /users
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
    @PutMapping("/users/{id}") // <-- DODANY /users
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
    @DeleteMapping("/users/{id}") // <-- DODANY /users
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


    // ----------------------------------------------------------------------
    // --- 2. ZARZĄDZANIE AKTUALIZACJAMI (/api/admin/update/...) ---
    // ----------------------------------------------------------------------

    // Wymaganie 5.1 (Android): Pobieranie statusu aktualizacji (Pełna ścieżka: /api/admin/update/status)
    @GetMapping("/update/status")
    public ResponseEntity<Map<String, String>> getUpdateStatus() {
        return ResponseEntity.ok(updateService.getUpdateStatuses());
    }

    // Wymaganie 5.2 (Android -> VPS): Zapisuje decyzję użytkownika (Pełna ścieżka: /api/admin/update/decision)
    @PostMapping("/update/decision")
    public ResponseEntity<Map<String, String>> postUpdateDecision(
            // Używamy @AuthenticationPrincipal do pobrania zalogowanego użytkownika z JWT
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> decisionRequest) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Brak autoryzacji użytkownika."));
        }

        String key = decisionRequest.get("key");
        String decision = decisionRequest.get("decision");

        if (key == null || decision == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Brak klucza lub decyzji."));
        }

        updateService.recordDecision(user.getEmail(), key, decision);

        return ResponseEntity.ok(Map.of("message", "Decyzja zapisana."));
    }

    // Wymaganie 5.1/5.2 (Admin Web): Ustawianie statusu przez Web UI
    @PostMapping("/update/set")
    public ResponseEntity<Map<String, String>> setUpdateStatus(@RequestBody UpdateStatusRequest request) {
        updateService.setUpdateStatus(request.key(), request.status(), request.shouldNotify());
        return ResponseEntity.ok(Map.of("message", String.format("Status dla %s ustawiony na %s.", request.key(), request.status())));
    }

}