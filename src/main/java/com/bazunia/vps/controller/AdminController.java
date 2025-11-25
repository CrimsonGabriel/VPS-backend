package com.bazunia.vps.controller;

import com.bazunia.vps.dto.*;
import com.bazunia.vps.model.*;
import com.bazunia.vps.repository.*;
import com.bazunia.vps.service.*;
import com.bazunia.vps.service.RetentionService.RetentionConfigDto;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final UpdateService updateService;
    private final RetentionService retentionService;
    private final SensorService sensorService;
    private final SystemSettingRepository systemSettingRepository;
    private final GatewayRepository gatewayRepository;

    // --- 2. ZARZĄDZANIE AKTUALIZACJAMI ---

    @PostMapping("/updates")
    public ResponseEntity<?> createUpdate(@RequestBody UpdateDtos.CreateUpdateRequest request) {
        // Wyjątek poleci do Handlera
        return ResponseEntity.ok(updateService.createUpdate(request));
    }

    @GetMapping("/updates")
    public ResponseEntity<List<UpdateDtos.UpdateSummaryDto>> getAllUpdates() {
        return ResponseEntity.ok(updateService.getAllUpdatesSummary());
    }

    @PostMapping("/updates/{assignmentId}/resend")
    public ResponseEntity<?> resendUpdate(@PathVariable Long assignmentId) {
        updateService.resendUpdate(assignmentId);
        return ResponseEntity.ok(Map.of("message", "Update resent to PENDING status."));
    }

    // Helper dla Frontendu
    @GetMapping("/users/{userId}/gateways")
    public ResponseEntity<List<GatewayDto>> getUserGateways(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<GatewayDto> dtos = gatewayRepository.findWithSensorsByOwner(user).stream()
                .map(GatewayDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/gateways/{gatewayId}/sensors")
    public ResponseEntity<List<SensorDto>> getGatewaySensors(@PathVariable Long gatewayId) {
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Gateway not found"));

        List<SensorDto> dtos = gateway.getSensors().stream()
                .map(SensorDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // --- 3. ZARZĄDZANIE SENSORAMI (Admin) ---

    @PostMapping("/sensors")
    public ResponseEntity<?> createSensor(@Valid @RequestBody SensorCreateRequest request) {
        Sensor savedSensor = sensorService.createSensor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SensorDto.fromEntity(savedSensor));
    }

    @PatchMapping("/sensors/{id}")
    public ResponseEntity<?> updateSensor(
            @PathVariable Long id,
            @Valid @RequestBody SensorUpdateRequest request) {
        Sensor updatedSensor = sensorService.updateSensor(id, request);
        return ResponseEntity.ok(SensorDto.fromEntity(updatedSensor));
    }

    // --- 5. KONFIGURACJA HISTORII (Retention) ---

    @GetMapping("/retention")
    public ResponseEntity<RetentionConfigDto> getRetentionSettings() {
        return ResponseEntity.ok(retentionService.getRetentionSettings());
    }

    @PostMapping("/retention")
    public ResponseEntity<?> saveRetentionSettings(@RequestBody RetentionConfigDto request) {
        retentionService.saveRetentionSettings(request.retentionDays(), request.maxRecords());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/retention/run")
    public ResponseEntity<?> runCleanupNow() {
        retentionService.performAutoCleanup();
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
                retentionService.saveRetentionSettings(Integer.parseInt(reqDays), null);
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

    // --- 6. STATUS BRAMEK (Admin View) ---

    @GetMapping("/gateways/status")
    @Transactional
    public ResponseEntity<List<AdminGatewayStatusDto>> getAllGatewaysStatus() {
        List<Gateway> gateways = gatewayRepository.findAll();

        List<AdminGatewayStatusDto> result = gateways.stream().map(gateway -> {
            String ownerEmail = (gateway.getOwner() != null) ? gateway.getOwner().getEmail() : "Brak właściciela";

            // --- SEKCJA POPRAWKI FOLDERU START ---
            // Zamiast gateway.getFolder(), pobieramy pierwszy folder z listy powiązanych
            String folderName = "Brak folderu";
            if (gateway.getLinkedFolders() != null && !gateway.getLinkedFolders().isEmpty()) {
                // Bierzemy pierwszy folder z brzegu (zakładając, że bramka jest w jednym folderze na raz)
                folderName = gateway.getLinkedFolders().iterator().next().getName();
            } else if (gateway.getFolder() != null) {
                // Fallback: jeśli relacja pusta, spróbujmy starego pola (opcjonalnie)
                folderName = gateway.getFolder();
            }
            // --- SEKCJA POPRAWKI FOLDERU KONIEC ---

            List<AdminSensorSummaryDto> sensors = gateway.getSensors().stream()
                    .map(s -> new AdminSensorSummaryDto(
                            s.getName(), s.getType(), s.getBatteryLevel(), s.isReportingEnabled()
                    )).collect(Collectors.toList());

            List<AdminSharedUserDto> sharedWith = new ArrayList<>();
            if (gateway.getPermissions() != null) {
                sharedWith = gateway.getPermissions().stream()
                        .map(perm -> new AdminSharedUserDto(
                                perm.getUser().getEmail(), perm.getPermissionLevel().name()
                        )).collect(Collectors.toList());
            }

            String lastSeenStr = (gateway.getLastSeen() != null) ? gateway.getLastSeen() + "Z" : null;
            // Tutaj podmieniamy parametr w konstruktorze: folderName zamiast gateway.getFolder()
            return new AdminGatewayStatusDto(
                    gateway.getId(),
                    gateway.getName(),
                    ownerEmail,
                    gateway.getStatus(),
                    lastSeenStr,
                    gateway.getDescription(),
                    folderName, // <--- TUTAJ WPADACIE NAZWA Z RELACJI
                    sensors,
                    sharedWith
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}