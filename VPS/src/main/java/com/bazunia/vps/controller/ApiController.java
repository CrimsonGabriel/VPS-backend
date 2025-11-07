// 💾 ApiController.java (Ostateczna Logika)
package com.bazunia.vps.controller;

import com.bazunia.vps.dto.TwoFaStatusDto;
import com.bazunia.vps.model.User;
import com.bazunia.vps.service.DataService;
import com.bazunia.vps.dto.DataRequest;
import com.bazunia.vps.dto.RegistrationRequest;
import com.bazunia.vps.dto.StatusResponse;
import com.bazunia.vps.dto.UpdateRequest;
import com.bazunia.vps.model.SensorReading;
import com.bazunia.vps.repository.SensorReadingRepository;
import com.bazunia.vps.repository.UserRepository;
import com.bazunia.vps.service.StatusService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ApiController {

    private static final String SECRET_PASSWORD = "ZMIEN_TO_HASLO_XD";

    private final StatusService statusService;
    private final SensorReadingRepository sensorRepository;
    private final UserRepository userRepository;
    private final DataService dataService;

    @Autowired
    public ApiController(StatusService statusService, SensorReadingRepository sensorRepository, UserRepository userRepository, DataService dataService) {
        this.statusService = statusService;
        this.sensorRepository = sensorRepository;
        this.userRepository = userRepository;
        this.dataService = dataService;
    }

    // --- ENDPOINT DLA RASPBERRY PI: REJESTRACJA IP (/register/rasp) ---
    @PostMapping("/register/rasp")
    public ResponseEntity<String> registerRaspberryPiIp(@RequestBody RegistrationRequest request, HttpServletRequest servletRequest) {
        if (!SECRET_PASSWORD.equals(request.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }

        String clientIp = servletRequest.getRemoteAddr();

        // Zapisujemy IP jako RPi (TO JEST CEL TEGO ENDPOINTU)
        statusService.updateRpiIp(clientIp);
        statusService.addRpiIp(clientIp); // DEDYKOWANY ZBIÓR DLA IP RPI

        System.out.println("Zarejestrowano IP RPi: " + clientIp + ", Port: " + request.port());
        return ResponseEntity.ok("OK");
    }

    // --- ENDPOINT DLA ANDROIDA: REJESTRACJA IP (/register/android) ---
    @PostMapping("/register/android")
    public ResponseEntity<String> registerAndroidIp(@RequestBody RegistrationRequest request, HttpServletRequest servletRequest) {
        if (!SECRET_PASSWORD.equals(request.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }

        String clientIp = servletRequest.getRemoteAddr();


        statusService.addAndroidIp(clientIp);

        System.out.println("Zarejestrowano IP Androida: " + clientIp + ", Port: " + request.port());
        return ResponseEntity.ok("OK");
    }

    // --- ENDPOINT DLA RASPBERRY PI: DANE (/data) ---
    @PostMapping("/data")
    public ResponseEntity<String> postData(@RequestBody DataRequest data, HttpServletRequest request) {
        if (!SECRET_PASSWORD.equals(data.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }

        String clientIp = request.getRemoteAddr();

        // Aktualizacja czasu meldunku i IP RPi

        statusService.updateRpiIp(clientIp);
        statusService.addRpiIp(clientIp); // DEDYKOWANY ZBIÓR DLA IP RPI

        data.sensors().stream()
                .map(s -> new SensorReading(
                        null,
                        s.getGatewayId(),
                        s.getSensorId(),
                        s.getType(),
                        s.getValue(),
                        s.getTimestamp()
                ))
                .forEach(sensorRepository::save);

        return ResponseEntity.ok("OK");
    }

    // --- ENDPOINT MELDUNKU (/update) ---
    @PostMapping("/update")
    public ResponseEntity<String> updateReport(@RequestBody UpdateRequest request) {
        if (!SECRET_PASSWORD.equals(request.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }

        statusService.updateLastReport(request.text());

        return ResponseEntity.ok("OK");
    }

    // --- POZOSTAŁE METODY BEZ ZMIAN ---
    @GetMapping("/data/android")
    public ResponseEntity<Map<String, List<SensorReading>>> getSensorData() {
        List<SensorReading> readings = sensorRepository.findTop10ByOrderByTimestampDesc();
        Map<String, List<SensorReading>> response = Map.of("sensors", readings);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/data/history/delete")
    public ResponseEntity<Map<String, Boolean>> deleteSensorHistory() {
        dataService.deleteAllSensorReadings();
        return ResponseEntity.ok(Map.of("success", true));
    }


    @GetMapping("/")
    public String getRootStatus() {
        return "Serwer VPS (Spring Boot) działa!";
    }

    @GetMapping("/status/json")
    public StatusResponse getStatusJson() {

        List<User> allUsers = userRepository.findAll();

        List<String> authorizedUsersList = allUsers.stream()
                .map(User::getEmail)
                .collect(Collectors.toList());

        Map<String, TwoFaStatusDto> users2FAStatusMap = allUsers.stream()
                .collect(Collectors.toMap(
                        User::getEmail,
                        user -> new TwoFaStatusDto(user.isTwoFactorEnabled())
                ));

        return new StatusResponse(
                statusService.getLastReportText(),
                statusService.getRegisteredRPiIp(),
                statusService.getUniqueAndroidIPs(), // ZBIÓR TYLKO IP ANDROIDA
                authorizedUsersList,
                users2FAStatusMap
        );
    }
}