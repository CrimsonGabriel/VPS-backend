package com.bazunia.vps.controller;

import com.bazunia.vps.dto.*;
import com.bazunia.vps.model.*;
import com.bazunia.vps.repository.*;
import com.bazunia.vps.service.*;
import com.bazunia.vps.service.DataService.RetentionConfigDto;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UpdateService updateService;
    private final DataService dataService;
    private final SensorService sensorService;
    private final SystemSettingRepository systemSettingRepository;

    // ⭐️ DODANO: Brakujące repozytorium (było używane w kodzie, ale niewstrzyknięte)
    private final GatewayRepository gatewayRepository;

    // --- DTOs ---

    public record AdminUserRequest(
            String email,
            String password,
            String name,
            boolean isAdmin,
            boolean enabled
    ) {}

//    public record UpdateStatusRequest(
//            String key,
//            String status,
//            boolean shouldNotify
//    ) {}

    // ⭐️ BEZPIECZNE DTO DLA LISTY USERÓW (To naprawia błąd "nesting depth" w UsersPage)
    public record AdminUserDto(
            Long id,
            String email,
            String name,
            String role,
            boolean enabled,
            boolean twoFactorEnabled
    ) {
        public static AdminUserDto fromEntity(User user) {
            return new AdminUserDto(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getRole().name(),
                    user.isEnabled(),
                    user.isTwoFactorEnabled()
            );
        }
    }

    // ----------------------------------------------------------------------
    // --- 1. ZARZĄDZANIE UŻYTKOWNIKAMI (/api/admin/users/...) ---
    // ----------------------------------------------------------------------

    // --- READ (Wszyscy Użytkownicy) ---
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        // ⭐️ ZMIANA: Zwracamy DTO zamiast encji, żeby przerwać pętlę JSON
        List<AdminUserDto> users = userRepository.findAll().stream()
                .map(AdminUserDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    // --- READ (Pobierz po ID) ---
    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDto> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(AdminUserDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- CREATE (Dodaj Użytkownika) ---
    @PostMapping("/users")
    public ResponseEntity<AdminUserDto> createUser(@RequestBody AdminUserRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (request.password() == null || request.password().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
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

    // --- UPDATE (Edytuj Użytkownika) ---
    @PutMapping("/users/{id}")
    public ResponseEntity<AdminUserDto> updateUser(@PathVariable Long id, @RequestBody AdminUserRequest request) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setEmail(request.email());
                    existingUser.setName(request.name());
                    existingUser.setRole(request.isAdmin() ? Role.ADMIN : Role.USER);
                    existingUser.setEnabled(request.enabled());

                    if (request.password() != null && !request.password().isEmpty()) {
                        existingUser.setPassword(passwordEncoder.encode(request.password()));
                    }

                    User saved = userRepository.save(existingUser);
                    return ResponseEntity.ok(AdminUserDto.fromEntity(saved));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- DELETE (Usuń Użytkownika) ---
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        Optional<User> userToDelete = userRepository.findById(id);

        if (userToDelete.isPresent() && userToDelete.get().getEmail().equals("admin")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }


    // ----------------------------------------------------------------------
    // --- 2. ZARZĄDZANIE AKTUALIZACJAMI ---
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

    // Helper dla Frontendu - pobiera bramki użytkownika (UpdatesPage.jsx)
    @GetMapping("/users/{userId}/gateways")
    public ResponseEntity<List<GatewayDto>> getUserGateways(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // ⭐️ Używamy GatewayDto.fromEntity, które jest bezpieczne (nie ma pętli)
        List<GatewayDto> dtos = gatewayRepository.findWithSensorsByOwner(user).stream()
                .map(GatewayDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ⭐️ DODANO: Helper dla Frontendu - pobiera sensory bramki (UpdatesPage.jsx tego potrzebuje!)
    @GetMapping("/gateways/{gatewayId}/sensors")
    public ResponseEntity<List<SensorDto>> getGatewaySensors(@PathVariable Long gatewayId) {
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Gateway not found"));

        List<SensorDto> dtos = gateway.getSensors().stream()
                .map(SensorDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ----------------------------------------------------------------------
    // --- 3. ZARZĄDZANIE SENSORAMI (/api/admin/sensors/...) ---
    // ----------------------------------------------------------------------

    @PostMapping("/sensors")
    public ResponseEntity<?> createSensor(@Valid @RequestBody SensorCreateRequest request) {
        try {
            Sensor savedSensor = sensorService.createSensor(request);
            // Zwracamy DTO
            return ResponseEntity.status(HttpStatus.CREATED).body(SensorDto.fromEntity(savedSensor));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/sensors/{id}")
    public ResponseEntity<?> updateSensor(
            @PathVariable Long id,
            @Valid @RequestBody SensorUpdateRequest request) {
        try {
            Sensor updatedSensor = sensorService.updateSensor(id, request);
            // Zwracamy DTO
            return ResponseEntity.ok(SensorDto.fromEntity(updatedSensor));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ----------------------------------------------------------------------
    // --- 4. MULTI-LOG SYSTEM (/api/admin/logs?type=...) ---
    // ----------------------------------------------------------------------

    @GetMapping("/logs")
    public ResponseEntity<List<String>> getAppLogs(@RequestParam(defaultValue = "out") String type) {
        String logFilePath = switch (type) {
            case "error" -> "/home/ubuntu/.pm2/logs/bazunia-app-spring-error.log";
            case "server" -> "server.log";
            default -> "/home/ubuntu/.pm2/logs/bazunia-app-spring-out.log";
        };

        int linesToRead = 300;
        try (Stream<String> lines = Files.lines(Paths.get(logFilePath))) {
            List<String> allLines = lines.toList();
            int start = Math.max(0, allLines.size() - linesToRead);
            List<String> lastLines = allLines.subList(start, allLines.size());

            Pattern ansiPattern = Pattern.compile("\\x1B\\[[0-9;]*[a-zA-Z]");
            List<String> cleanLogs = lastLines.stream()
                    .map(line -> ansiPattern.matcher(line).replaceAll(""))
                    .toList();

            return ResponseEntity.ok(cleanLogs);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Błąd odczytu: " + e.getMessage()));
        }
    }

    // ----------------------------------------------------------------------
    // --- 5. KONFIGURACJA HISTORII (Retention Policy) ---
    // ----------------------------------------------------------------------

    @GetMapping("/retention")
    public ResponseEntity<RetentionConfigDto> getRetentionSettings() {
        return ResponseEntity.ok(dataService.getRetentionSettings());
    }

    @PostMapping("/retention")
    public ResponseEntity<?> saveRetentionSettings(@RequestBody RetentionConfigDto request) {
        dataService.saveRetentionSettings(request.retentionDays(), request.maxRecords());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/retention/run")
    public ResponseEntity<?> runCleanupNow() {
        dataService.performAutoCleanup();
        return ResponseEntity.ok(Map.of("message", "Czyszczenie uruchomione w tle."));
    }

    @GetMapping("/retention/request")
    public ResponseEntity<Map<String, String>> getPendingRetentionRequest() {
        String status = systemSettingRepository.findById("RETENTION_REQ_STATUS").map(SystemSetting::getValue).orElse("NONE");
        String reqDays = systemSettingRepository.findById("RETENTION_REQ_DAYS").map(SystemSetting::getValue).orElse(null);
        String reqSize = systemSettingRepository.findById("RETENTION_REQ_SIZE").map(SystemSetting::getValue).orElse(null);

        return ResponseEntity.ok(Map.of(
                "status", status,
                "reqDays", reqDays != null ? reqDays : "",
                "reqSize", reqSize != null ? reqSize : ""
        ));
    }

    @PostMapping("/retention/decision")
    @Transactional
    public ResponseEntity<?> handleRetentionDecision(@RequestBody Map<String, String> body) {
        String decision = body.get("decision");

        if ("ACCEPT".equals(decision)) {
            String reqDays = systemSettingRepository.findById("RETENTION_REQ_DAYS").map(SystemSetting::getValue).orElse(null);
            String reqSize = systemSettingRepository.findById("RETENTION_REQ_SIZE").map(SystemSetting::getValue).orElse(null);

            if (reqDays != null && !reqDays.isEmpty()) {
                dataService.saveRetentionSettings(Integer.parseInt(reqDays), null);
            }
            if (reqSize != null && !reqSize.isEmpty()) {
                systemSettingRepository.save(new SystemSetting("RETENTION_MAX_RECORDS", reqSize));
            }

            systemSettingRepository.save(new SystemSetting("RETENTION_REQ_STATUS", "ACCEPTED"));

        } else if ("REJECT".equals(decision)) {
            systemSettingRepository.save(new SystemSetting("RETENTION_REQ_STATUS", "REJECTED"));
        } else {
            return ResponseEntity.badRequest().body("Nieznana decyzja. Użyj ACCEPT lub REJECT.");
        }

        return ResponseEntity.ok(Map.of("message", "Decyzja zapisana: " + decision));
    }
    // ----------------------------------------------------------------------
    // --- 6. MONITOROWANIE STATUSU BRAMEK (NOWOŚĆ) ---
    // ----------------------------------------------------------------------

    // DTO tylko dla widoku statusu admina
    public record AdminSensorSummaryDto(
            String name,
            String type,
            Integer batteryLevel,
            boolean reportingEnabled
    ) {}

    public record AdminSharedUserDto(
            String email,
            String permissionLevel
    ) {}

    public record AdminGatewayStatusDto(
            Long id,
            String name,
            String ownerEmail,
            String status,       // np. ONLINE, OFFLINE
            String lastSeen,     // Data ostatniej aktywności
            String description,
            String folder,
            List<AdminSensorSummaryDto> sensors,
            List<AdminSharedUserDto> sharedWith
    ) {}

    @GetMapping("/gateways/status")
    @Transactional // Ważne dla Lazy Loading (pobieranie permissions)
    public ResponseEntity<List<AdminGatewayStatusDto>> getAllGatewaysStatus() {
        List<Gateway> gateways = gatewayRepository.findAll();

        List<AdminGatewayStatusDto> result = gateways.stream().map(gateway -> {

            // 1. Właściciel
            String ownerEmail = (gateway.getOwner() != null) ? gateway.getOwner().getEmail() : "Brak właściciela";

            // 2. Sensory (bateria i nazwa)
            List<AdminSensorSummaryDto> sensors = gateway.getSensors().stream()
                    .map(s -> new AdminSensorSummaryDto(
                            s.getName(),
                            s.getType(),
                            s.getBatteryLevel(),
                            s.isReportingEnabled()
                    ))
                    .collect(Collectors.toList());

            // 3. Udostępnienia (komu udostępniono)
            List<AdminSharedUserDto> sharedWith = new ArrayList<>();
            if (gateway.getPermissions() != null) {
                sharedWith = gateway.getPermissions().stream()
                        .map(perm -> new AdminSharedUserDto(
                                perm.getUser().getEmail(),
                                perm.getPermissionLevel().name()
                        ))
                        .collect(Collectors.toList());
            }

            // 4. Formatowanie daty
            String lastSeenStr = (gateway.getLastSeen() != null)
                    ? gateway.getLastSeen().toString()
                    : null;

            return new AdminGatewayStatusDto(
                    gateway.getId(),
                    gateway.getName(),
                    ownerEmail,
                    gateway.getStatus(),
                    lastSeenStr,
                    gateway.getDescription(),
                    gateway.getFolder(),
                    sensors,
                    sharedWith
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}