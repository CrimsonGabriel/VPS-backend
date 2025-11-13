package com.bazunia.vps.controller;

import com.bazunia.vps.dto.TwoFaStatusDto;
import com.bazunia.vps.model.User;
import java.util.stream.Collectors;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
public class ApiController {

    private static final String SECRET_PASSWORD = "ZMIEN_TO_HASLO_XD";

    private final StatusService statusService;
    private final SensorReadingRepository sensorRepository;
    private final UserRepository userRepository;

    @Autowired
    public ApiController(StatusService statusService, SensorReadingRepository sensorRepository, UserRepository userRepository) {
        this.statusService = statusService;
        this.sensorRepository = sensorRepository;
        this.userRepository = userRepository;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedIp = request.getHeader("X-Forwarded-For");
        if (forwardedIp == null || forwardedIp.isEmpty()) {
            return request.getRemoteAddr();
        }
        return forwardedIp.split(",")[0].trim();
    }

    // --- ENDPOINTY RPi (zostają bez zmian) ---

    @PostMapping("/register/rasp")
    public ResponseEntity<String> registerRaspberry(@RequestBody RegistrationRequest body, HttpServletRequest request) {
        if (!SECRET_PASSWORD.equals(body.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nieprawidlowe haslo.");
        }
        String ip = getClientIp(request);
        statusService.updateRpiIp(ip + ":" + body.port());
        return ResponseEntity.ok("IP RPi zarejestrowane.");
    }

    @PostMapping("/register/android")
    public ResponseEntity<String> registerAndroid(@RequestBody RegistrationRequest body, HttpServletRequest request) {
        if (!SECRET_PASSWORD.equals(body.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nieprawidlowe haslo.");
        }
        String ip = getClientIp(request);
        statusService.updateAndroidIp(ip + ":" + body.port());
        return ResponseEntity.ok("IP Androida (z RPi) zarejestrowane.");
    }

    @PostMapping("/data")
    public ResponseEntity<String> receiveData(@RequestBody DataRequest body) {
        if (!SECRET_PASSWORD.equals(body.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nieprawidlowe haslo.");
        }
        sensorRepository.saveAll(body.sensors());
        return ResponseEntity.ok("Dane czujnikow zapisane w bazie.");
    }

    @PostMapping("/update")
    public ResponseEntity<String> receiveUpdate(@RequestBody UpdateRequest body, HttpServletRequest request) {
        if (!SECRET_PASSWORD.equals(body.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nieprawidlowe haslo.");
        }
        String senderIp = getClientIp(request);
        String sender = (statusService.getRegisteredRPiIp().contains(senderIp)) ? "RPi" : "Inne";
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String report;
        if (body.message() != null && !body.message().isEmpty()) {
            report = String.format("%s (Nadawca: %s): %s", now, sender, body.message());
        } else {
            report = String.format("%s (Nadawca: %s)", now, sender);
        }
        statusService.updateLastReport(report);
        return ResponseEntity.ok("Meldunek odebrany pomyslnie.");
    }

    // --- ⭐️ POPRAWIONY ENDPOINT DLA ANDROIDA (dla błędu JSON) ⭐️ ---
    @GetMapping("/data/android")
    public ResponseEntity<?> getSensorData() {
        // Zmieniamy na pobieranie tylko najnowszych 10 wpisów
        // To jest bardziej wydajne i jest bliższe logice z Node.js (ostatnie dane)
        List<SensorReading> readings = sensorRepository.findTop10ByOrderByTimestampDesc();
        Map<String, List<SensorReading>> response = Map.of("sensors", readings);
        return ResponseEntity.ok(response);
    }

    // --- ENDPOINTY STATUSOWE (zostają bez zmian) ---
    @GetMapping("/")
    public String getRootStatus() {
        return "Serwer VPS (Spring Boot) działa!";
    }

    @GetMapping("/status/json")
    public StatusResponse getStatusJson() {

        // 1. Pobierz wszystkich użytkowników z bazy
        List<User> allUsers = userRepository.findAll();

        // 2. Stwórz listę samych e-maili (jak w server.js "authorizedUsers")
        List<String> authorizedUsersList = allUsers.stream()
                .map(User::getEmail)
                .collect(Collectors.toList());

        // 3. Stwórz mapę statusów 2FA (jak w server.js "users2FAStatus")
        Map<String, TwoFaStatusDto> users2FAStatusMap = allUsers.stream()
                .collect(Collectors.toMap(
                        User::getEmail,
                        user -> new TwoFaStatusDto(user.isTwoFactorEnabled())
                ));

        // 4. Zwróć nowy DTO z poprawnymi nazwami pól
        return new StatusResponse(
                statusService.getLastReportText(),
                statusService.getRegisteredRPiIp(),
                statusService.getUniqueAndroidIPs(), // (Nazwa w DTO to 'registeredAndroidIps')
                authorizedUsersList,
                users2FAStatusMap
        );
    }
}

