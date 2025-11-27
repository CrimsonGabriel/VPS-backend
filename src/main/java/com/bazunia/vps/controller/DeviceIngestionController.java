package com.bazunia.vps.controller;

import com.bazunia.vps.dto.*;
import com.bazunia.vps.model.*;
import com.bazunia.vps.repository.*;
import com.bazunia.vps.service.GatewayService;
import com.bazunia.vps.service.StatusService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DeviceIngestionController {

    @org.springframework.beans.factory.annotation.Value("${app.device.secret}")
    private String secretPassword;

    private final StatusService statusService;
    private final GatewayService gatewayService;
    private final GatewayRepository gatewayRepository;
    private final SensorRepository sensorRepository;
    private final SensorReadingRepository sensorReadingRepository;

    @PostMapping("/register/rasp")
    public ResponseEntity<String> registerRaspberryPiIp(@RequestBody RegistrationRequest request, HttpServletRequest servletRequest) {
        if (!this.secretPassword.equals(request.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }
        String clientIp = servletRequest.getRemoteAddr();
        statusService.updateRpiIp(clientIp);
        statusService.addRpiIp(clientIp);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/register/android")
    public ResponseEntity<String> registerAndroidIp(@RequestBody RegistrationRequest request, HttpServletRequest servletRequest) {
        if (!this.secretPassword.equals(request.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }
        String clientIp = servletRequest.getRemoteAddr();
        statusService.addAndroidIp(clientIp);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/data")
    public ResponseEntity<String> postData(@RequestBody SimpleDataRequest data, HttpServletRequest request) {
        if (!this.secretPassword.equals(data.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }
        String clientIp = request.getRemoteAddr();
        statusService.updateRpiIp(clientIp);
        statusService.addRpiIp(clientIp);

        for (var sensorDto : data.sensors()) {
            Long gatewayId = Long.parseLong(sensorDto.gateway_id());
            Long sensorId = Long.parseLong(sensorDto.sensor_id());

            Gateway gateway = gatewayRepository.findById(gatewayId)
                    .orElseThrow(() -> new RuntimeException("Nie znaleziono bramki ID: " + gatewayId));

            gateway.setStatus("online");
            gateway.setLastSeen(java.time.LocalDateTime.now());
            gatewayRepository.save(gateway);

            Sensor sensor = sensorRepository.findById(sensorId)
                    .orElseThrow(() -> new RuntimeException("Nie znaleziono czujnika ID: " + sensorId));

            SensorReading newReading = new SensorReading();
            newReading.setGateway(gateway);
            newReading.setSensor(sensor);
            newReading.setValue(sensorDto.value());
            newReading.setTimestamp(sensorDto.timestamp());

            sensorReadingRepository.save(newReading);
        }

        return ResponseEntity.ok("OK");
    }

    @PostMapping("/api/sensors/battery")
    @Transactional
    public ResponseEntity<String> updateBatteryStatuses(@RequestBody BatteryUpdateRequest request) {
        if (!this.secretPassword.equals(request.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }
        if (request.statuses() == null || request.statuses().isEmpty()) {
            return new ResponseEntity<>("Brak statusów", HttpStatus.BAD_REQUEST);
        }

        int updatedCount = 0;
        for (var statusDto : request.statuses()) {
            if (statusDto.sensorId() == null || statusDto.level() == null) continue;

            var sensorOpt = sensorRepository.findById(statusDto.sensorId());
            if (sensorOpt.isPresent()) {
                Sensor sensor = sensorOpt.get();
                sensor.setBatteryLevel(statusDto.level());
                sensorRepository.save(sensor);
                updatedCount++;
            }
        }
        return ResponseEntity.ok("Zaktualizowano baterię dla " + updatedCount + " czujników.");
    }

    @PostMapping("/api/sensors/config")
    public ResponseEntity<?> getSensorConfig(@RequestBody PasswordRequest request) {
        if (!this.secretPassword.equals(request.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(gatewayService.getAllSensorConfigs());
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateReport(@RequestBody UpdateRequest request) {
        if (!this.secretPassword.equals(request.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }
        statusService.updateLastReport(request.text());
        return ResponseEntity.ok("OK");
    }
}