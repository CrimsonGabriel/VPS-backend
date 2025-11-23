package com.bazunia.vps.controller;

import com.bazunia.vps.dto.*;
import com.bazunia.vps.model.*;
import com.bazunia.vps.repository.*;
import com.bazunia.vps.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final StatusService statusService;
    private final MonitoringService monitoringService;
    private final RetentionService retentionService;
    private final UpdateService updateService;
    private final SensorReadingRepository sensorReadingRepository;
    private final UserRepository userRepository;
    private final SystemSettingRepository systemSettingRepository;

    @GetMapping("/")
    public String getRootStatus() {
        return "Serwer VPS (Spring Boot) działa!";
    }

    @GetMapping("/status/json")
    public StatusResponse getStatusJson() {
        List<User> allUsers = userRepository.findAll();
        List<String> authorizedUsersList = allUsers.stream().map(User::getEmail).collect(Collectors.toList());
        Map<String, TwoFaStatusDto> users2FA = allUsers.stream().collect(Collectors.toMap(
                User::getEmail, u -> new TwoFaStatusDto(u.isTwoFactorEnabled())
        ));

        // Tuta przekazujemy brakujący parametr: statusService.getUniqueRpiIPs()
        return new StatusResponse(
                statusService.getLastReportText(),
                statusService.getRegisteredRPiIp(),
                statusService.getUniqueAndroidIPs(),
                statusService.getUniqueRpiIPs(), // <--- DODANE
                authorizedUsersList,
                users2FA
        );
    }

    @GetMapping("/api/sensors/status")
    public ResponseEntity<List<SensorStatusErrorDto>> getSensorStatuses(Authentication authentication) {
        return ResponseEntity.ok(monitoringService.checkSensorStatuses(getAuthenticatedUser(authentication)));
    }

    @GetMapping("/api/status/risk-report")
    public ResponseEntity<RiskReportDto> getRiskReport(Authentication authentication) {
        return ResponseEntity.ok(monitoringService.generateRiskReport(getAuthenticatedUser(authentication)));
    }

    // --- ANDROID / APP DASHBOARD ---

    @GetMapping("/data/android")
    public ResponseEntity<Map<String, List<SensorReadingResponseDto>>> getSensorData() {
        var readings = sensorReadingRepository.findLatestReadingsWithDetails(PageRequest.of(0, 10));
        return ResponseEntity.ok(Map.of("sensors", readings));
    }

    @GetMapping("/api/my-updates")
    public ResponseEntity<List<UpdateDtos.ClientUpdateResponse>> getMyUpdates(Authentication authentication) {
        return ResponseEntity.ok(updateService.getPendingUpdatesForUser(getAuthenticatedUser(authentication).getId()));
    }

    @PostMapping("/api/updates/{assignmentId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long assignmentId, @RequestBody UpdateDtos.UpdateStatusRequest request) {
        try {
            updateService.updateAssignmentStatus(assignmentId, request.getStatus());
            return ResponseEntity.ok(Map.of("message", "Status updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- RETENTION REQUESTS ---

    @PostMapping("/api/retention/request")
    public ResponseEntity<?> requestRetentionChange(@RequestBody Map<String, String> request) {
        String days = request.get("days");
        String size = request.get("size");

        if (days == null && size == null) return ResponseEntity.badRequest().body("Musisz podać dni lub rozmiar.");

        if (days != null) systemSettingRepository.save(new SystemSetting("RETENTION_REQ_DAYS", days));
        if (size != null) systemSettingRepository.save(new SystemSetting("RETENTION_REQ_SIZE", size));
        systemSettingRepository.save(new SystemSetting("RETENTION_REQ_STATUS", "PENDING"));

        return ResponseEntity.ok(Map.of("message", "Wniosek wysłany."));
    }

    @GetMapping("/api/retention/status")
    public ResponseEntity<?> getRetentionRequestStatus() {
        String status = systemSettingRepository.findById("RETENTION_REQ_STATUS").map(SystemSetting::getValue).orElse("NONE");
        String currentDays = systemSettingRepository.findById("RETENTION_DAYS").map(SystemSetting::getValue).orElse("0");
        String currentSize = systemSettingRepository.findById("RETENTION_MAX_RECORDS").map(SystemSetting::getValue).orElse("0");

        return ResponseEntity.ok(Map.of("status", status, "currentDays", currentDays, "currentSize", currentSize));
    }

    // --- HISTORY CLEANUP ---

    @DeleteMapping("/api/data/history/delete")
    public ResponseEntity<Map<String, Boolean>> deleteSensorHistory() {
        retentionService.deleteAllSensorReadings();
        return ResponseEntity.ok(Map.of("success", true));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}