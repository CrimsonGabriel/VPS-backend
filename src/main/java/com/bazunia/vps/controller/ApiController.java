package com.bazunia.vps.controller;

import com.bazunia.vps.dto.DataRequest;
import com.bazunia.vps.dto.RegistrationRequest;
import com.bazunia.vps.dto.StatusResponse;
import com.bazunia.vps.dto.UpdateRequest; // 1. NOWY IMPORT
import com.bazunia.vps.repository.SensorReadingRepository;
import com.bazunia.vps.service.StatusService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
public class ApiController {

    // Hasło tylko dla RPi
    private static final String SECRET_PASSWORD = "ZMIEN_TO_HASLO_XD";

    private final StatusService statusService;
    private final SensorReadingRepository sensorRepository;

    @Autowired
    public ApiController(StatusService statusService, SensorReadingRepository sensorRepository) {
        this.statusService = statusService;
        this.sensorRepository = sensorRepository;
    }

    // --- ENDPOINTY DLA RASPBERRY PI (Ręczna walidacja hasła) ---

    @PostMapping("/register/rasp")
    public ResponseEntity<String> registerRaspberry(@RequestBody RegistrationRequest body, HttpServletRequest request) {
        if (!SECRET_PASSWORD.equals(body.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nieprawidlowe haslo.");
        }
        String ip = request.getRemoteAddr(); // Pobranie IP
        statusService.updateRpiIp(ip + ":" + body.port());
        return ResponseEntity.ok("IP RPi zarejestrowane.");
    }

    @PostMapping("/register/android")
    public ResponseEntity<String> registerAndroid(@RequestBody RegistrationRequest body, HttpServletRequest request) {
        // Ten endpoint jest też używany przez RPi (stary skrypt)
        if (!SECRET_PASSWORD.equals(body.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nieprawidlowe haslo.");
        }
        String ip = request.getRemoteAddr();
        // Logika z server.js rejestrowała to jako "Android IP"
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

    // 2. NOWY ENDPOINT (Przeniesiony z server.js)
    @PostMapping("/update")
    public ResponseEntity<String> receiveUpdate(@RequestBody UpdateRequest body, HttpServletRequest request) {
        if (!SECRET_PASSWORD.equals(body.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nieprawidlowe haslo.");
        }

        String senderIp = request.getRemoteAddr();
        // Sprawdź, czy IP pasuje do zarejestrowanego RPi
        String sender = (statusService.getRegisteredRPiIp().contains(senderIp)) ? "RPi" : "Inne";
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String report;

        if (body.message() != null && !body.message().isEmpty()) {
            report = String.format("%s (Nadawca: %s): %s", now, sender, body.message());
        } else {
            report = String.format("%s (Nadawca: %s)", now, sender);
        }

        statusService.updateLastReport(report); // Aktualizuj status
        return ResponseEntity.ok("Meldunek odebrany pomyslnie.");
    }


    // --- ENDPOINTY DLA APLIKACJI ANDROID / REACT (Chronione przez JWT) ---

    @GetMapping("/data/android")
    public ResponseEntity<?> getSensorData() {
        // 3. USUNIĘTA WERYFIKACJA HASŁA
        // Spring Security i filtr JWT zajmą się autoryzacją.
        // Jeśli kod dotarł tutaj, użytkownik jest uwierzytelniony.

        // Zwracamy wszystkie odczyty (w przyszłości można to filtrować)
        return ResponseEntity.ok(sensorRepository.findAll());
    }


    // --- ENDPOINTY STATUSOWE (Publiczne) ---

    @GetMapping("/")
    public String getRootStatus() {
        // TODO: Zaimplementować serwowanie pliku index.html
        // z src/main/resources/static
        return "Serwer VPS (Spring Boot) działa!";
    }

    @GetMapping("/status/json")
    public StatusResponse getStatusJson() {
        // 4. ZAKTUALIZOWANA ODPOWIEDŹ
        // TODO: Musisz zaktualizować StatusService, aby pobierał
        // listę IP Androida i statusy 2FA z bazy danych (z tabeli User)
        return new StatusResponse(
                statusService.getLastReportText(),
                statusService.getRegisteredRPiIp(),
                "PLACEHOLDER: Lista IP Androida z bazy" // Placeholder
                // Tutaj dojdą też statusy 2FA, gdy zaktualizujesz DTO
        );
    }
}