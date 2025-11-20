package com.bazunia.vps.controller;
import com.bazunia.vps.dto.UpdateDtos;
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
import java.util.Map;
import java.util.Optional;
import com.bazunia.vps.dto.SensorUpdateRequest;
import com.bazunia.vps.model.Sensor;
import com.bazunia.vps.model.Gateway;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import com.bazunia.vps.service.SensorService;
import com.bazunia.vps.dto.SensorCreateRequest;
import java.util.regex.Pattern;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.ArrayList;

@RestController
// Zmieniamy ścieżkę bazową na /api/admin, aby kontroler obsługiwał też inne sekcje admina
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UpdateService updateService;
    private final DataService dataService;


    private final SensorService sensorService;

    // DTOs dla uproszczenia (możesz je przenieść do pakietu dto)
    public record AdminUserRequest(
            String email,
            String password, // Opcjonalne: jeśli podane, hasło zostanie zmienione
            String name,
            boolean isAdmin,
            boolean enabled
    ) {
    }

    // DTOs dla aktualizacji
    public record UpdateStatusRequest(
            String key,     // "App", "GW-01", "Sensor-X"
            String status,  // "REQUIRED", "OPTIONAL", "NONE"
            boolean shouldNotify // Czy Android ma wyświetlić powiadomienie
    ) {
    }


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
        newUser.setEnabled(request.enabled());
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
                    existingUser.setEnabled(request.enabled());

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
    // --- 2. ZARZĄDZANIE AKTUALIZACJAMI (NOWA IMPLEMENTACJA) ---
    // ----------------------------------------------------------------------

    @PostMapping("/updates")
    public ResponseEntity<?> createUpdate(@RequestBody UpdateDtos.CreateUpdateRequest request) {
        try {
            return ResponseEntity.ok(updateService.createUpdate(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/updates")
    public ResponseEntity<List<UpdateDtos.UpdateSummaryDto>> getAllUpdates() {
        return ResponseEntity.ok(updateService.getAllUpdatesSummary());
    }

    @PostMapping("/updates/{assignmentId}/resend")
    public ResponseEntity<?> resendUpdate(@PathVariable Long assignmentId) {
        try {
            updateService.resendUpdate(assignmentId);
            return ResponseEntity.ok(Map.of("message", "Update resent to PENDING status."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Helper dla Frontendu - pobiera bramki użytkownika do dropdowna
    @GetMapping("/users/{userId}/gateways")
    public ResponseEntity<?> getGatewaysForUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(dataService.getGatewaysForUser(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

// ----------------------------------------------------------------------
    // --- 3. ZARZĄDZANIE SENSORAMI (/api/admin/sensors/...) ---
    // ----------------------------------------------------------------------

    /**
     * Tworzy nowy sensor z ręcznie podanym ID.
     * Sprawdza, czy sensor o takim ID już istnieje.
     */
    // ⭐️⭐️⭐️ DODAJ CAŁĄ TĘ METODĘ ⭐️⭐️⭐️
    @PostMapping("/sensors")
    public ResponseEntity<?> createSensor(@Valid @RequestBody SensorCreateRequest request) {
        try {
            // Cała logika jest teraz w serwisie
            Sensor savedSensor = sensorService.createSensor(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedSensor);

        } catch (RuntimeException e) {
            // Serwis rzuci wyjątek (np. "Sensor już istnieje" lub "Bramka nie istnieje")
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
// ----------------------------------------------------------------------
    // ⭐️⭐️⭐️ DODAJ CAŁĄ TĘ METODĘ ⭐️⭐️⭐️
    // ----------------------------------------------------------------------

    /**
     * Aktualizuje wybrane pola sensora o podanym ID.
     */
    @PatchMapping("/sensors/{id}")
    public ResponseEntity<?> updateSensor(
            @PathVariable Long id,
            @Valid @RequestBody SensorUpdateRequest request) {

        try {
            // Wywołaj logikę z serwisu
            Sensor updatedSensor = sensorService.updateSensor(id, request);
            return ResponseEntity.ok(updatedSensor); // Zwróć 200 OK z zaktualizowanym obiektem

        } catch (RuntimeException e) {
            // Przechwyć błędy (np. "Sensor nie istnieje", "Bramka nie istnieje")
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ----------------------------------------------------------------------
    // --- 4. MULTI-LOG SYSTEM (/api/admin/logs?type=...) ---
    // ----------------------------------------------------------------------

    @GetMapping("/logs")
    public ResponseEntity<List<String>> getAppLogs(
            @RequestParam(defaultValue = "out") String type // Domyślnie PM2 Output
    ) {
        String logFilePath;

        // Wybór pliku na podstawie parametru ?type=
        switch (type) {
            case "error":
                // Ścieżka z Twojego screena (Error Log)
                logFilePath = "/home/ubuntu/.pm2/logs/bazunia-app-spring-error.log";
                break;
            case "server":
                // Plik wewnętrzny Springa (w katalogu roboczym)
                logFilePath = "server.log";
                break;
            case "out":
            default:
                // Ścieżka z Twojego screena (Out Log)
                logFilePath = "/home/ubuntu/.pm2/logs/bazunia-app-spring-out.log";
                break;
        }

        int linesToRead = 300; // Czytamy ostatnie 300 linii

        try (Stream<String> lines = Files.lines(Paths.get(logFilePath))) {
            List<String> allLines = lines.toList();
            List<String> lastLines = new ArrayList<>();

            int start = Math.max(0, allLines.size() - linesToRead);
            lastLines = allLines.subList(start, allLines.size());

            // Usuwanie kodów kolorów ANSI (PM2 je dodaje, a w przeglądarce wyglądają jak śmieci [32m...)
            Pattern ansiPattern = Pattern.compile("\\x1B\\[[0-9;]*[a-zA-Z]");
            List<String> cleanLogs = lastLines.stream()
                    .map(line -> ansiPattern.matcher(line).replaceAll(""))
                    .toList();

            return ResponseEntity.ok(cleanLogs);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(
                            "Błąd odczytu pliku logów (" + type + "): " + e.getMessage(),
                            "Sprawdzana ścieżka: " + logFilePath,
                            "Upewnij się, że plik istnieje na serwerze VPS."
                    ));
        }
    }
}