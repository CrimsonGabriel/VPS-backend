// Plik: com/bazunia/vps/controller/ApiController.java
package com.bazunia.vps.controller;

import com.bazunia.vps.dto.TwoFaStatusDto;
import com.bazunia.vps.model.User;
import com.bazunia.vps.service.DataService;
import com.bazunia.vps.dto.DataRequest;
import com.bazunia.vps.dto.RegistrationRequest;
import com.bazunia.vps.dto.StatusResponse;
import com.bazunia.vps.dto.UpdateRequest;
import com.bazunia.vps.dto.SetPasswordRequest;
import com.bazunia.vps.dto.ChangePasswordRequest;
import com.bazunia.vps.dto.UserDetailsResponse;
import com.bazunia.vps.model.SensorReading;
import com.bazunia.vps.repository.SensorReadingRepository;
import com.bazunia.vps.repository.UserRepository;
import com.bazunia.vps.service.StatusService;
import com.bazunia.vps.service.UpdateService;
import com.bazunia.vps.service.AuthService;
import org.springframework.security.core.context.SecurityContextHolder;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bazunia.vps.service.JwtService;

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


    private final UpdateService updateService;
    private final JwtService jwtService;
    private final AuthService authService;
    @Autowired
    public ApiController(StatusService statusService,
                         SensorReadingRepository sensorRepository,
                         UserRepository userRepository,
                         DataService dataService,
                         UpdateService updateService,
                         JwtService jwtService, AuthService authService) {
        this.statusService = statusService;
        this.sensorRepository = sensorRepository;
        this.userRepository = userRepository;
        this.dataService = dataService;
        this.updateService = updateService;
        this.jwtService = jwtService;
        this.authService = authService;
    }

    // ⭐️⭐️⭐️ NOWY ENDPOINT DEBUGUJĄCY ⭐️⭐️⭐️
    @GetMapping("/debug-key")
    public ResponseEntity<Map<String, String>> debugJwtKey() {
        String actualKey = jwtService.getSecretKeyForDebug();

        System.out.println("==========================================================");
        System.out.println("== DEBUG: KLUCZ, KTÓREGO UŻYWA JwtService ==");
        System.out.println(actualKey);
        System.out.println("==========================================================");

        // Zwraca klucz w Base64 (to ten sam, który masz w properties)
        return ResponseEntity.ok(Map.of("key_in_use", actualKey));
    }

    // --- ENDPOINT DLA RASPBERRY PI: REJESTRACJA IP (/register/rasp) ---
    @PostMapping("/register/rasp")
    public ResponseEntity<String> registerRaspberryPiIp(@RequestBody RegistrationRequest request, HttpServletRequest servletRequest) {
        if (!SECRET_PASSWORD.equals(request.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }
        String clientIp = servletRequest.getRemoteAddr();
        statusService.updateRpiIp(clientIp);
        statusService.addRpiIp(clientIp);
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

    // --- ENDPOINTY DLA AKTUALIZACJI ANDROIDA ---

    // 1. ENDPOINT: POBIERANIE STATUSU AKTUALIZACJI (Rozwiązuje 403 na GET /api/update/status)
    @GetMapping("/api/update/status")
    public ResponseEntity<Map<String, String>> getUpdateStatus() {
        // Ta metoda teraz zadziała, bo updateService jest poprawnie wstrzyknięty
        Map<String, String> statusMap = updateService.getUpdateStatuses();
        return ResponseEntity.ok(statusMap);
    }

    // 2. ENDPOINT: DECYZJA UŻYTKOWNIKA (ACCEPTED/DEFERRED) (Rozwiązuje 403 na POST /api/update/decision)
    // ⭐️ POPRAWKA: Usunięto 'Principal principal' i zastąpiono go SecurityContextHolder
    @PostMapping("/api/update/decision")
    public ResponseEntity<String> recordUpdateDecision(
            @RequestBody Map<String, String> decisionPayload) {

        // 1. Walidacja danych
        String key = decisionPayload.get("key");
        String decision = decisionPayload.get("decision");

        // ⭐️ POPRAWKA: Pobieranie emaila użytkownika z kontekstu bezpieczeństwa
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        if (key == null || decision == null) {
            return new ResponseEntity<>("Brak 'key' lub 'decision' w ładunku", HttpStatus.BAD_REQUEST);
        }

        // 2. Wywołanie logiki UpdateService
        updateService.recordDecision(userEmail, key, decision);

        return ResponseEntity.ok("Decyzja o aktualizacji (" + decision + ") została zapisana.");
    }

    // --- ENDPOINT DLA RASPBERRY PI: DANE (/data) ---
    @PostMapping("/data")
    public ResponseEntity<String> postData(@RequestBody DataRequest data, HttpServletRequest request) {
        if (!SECRET_PASSWORD.equals(data.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }
        String clientIp = request.getRemoteAddr();
        statusService.updateRpiIp(clientIp);
        statusService.addRpiIp(clientIp);
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
        // Używamy .text() zgodnie z poprawką w UpdateRequest.java
        statusService.updateLastReport(request.text());
        return ResponseEntity.ok("OK");
    }

    // --- POZOSTAŁE ENDPOINTY (BEZ ZMIAN) ---

    // Endpoint do pobierania danych przez autoryzowanego użytkownika (Android)
    @GetMapping("/data/android")
    public ResponseEntity<Map<String, List<SensorReading>>> getSensorData() {
        List<SensorReading> readings = sensorRepository.findTop10ByOrderByTimestampDesc();
        Map<String, List<SensorReading>> response = Map.of("sensors", readings);
        return ResponseEntity.ok(response);
    }

    // Endpoint do usuwania historii (dostępny tylko dla ADMINA)
    @DeleteMapping("/api/data/history/delete")
    public ResponseEntity<Map<String, Boolean>> deleteSensorHistory() {
        dataService.deleteAllSensorReadings();
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ⭐️⭐️ NOWY ENDPOINT ⭐️⭐️
    /**
     * Ustawia hasło użytkownika.
     * Endpoint jest chroniony i wymaga ważnego tokena JWT.
     */
    @PostMapping("/api/user/set-password")
    public ResponseEntity<?> setUserPassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SetPasswordRequest request) {

        try {
            // Wyciągamy token JWT z nagłówka
            String jwtToken = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwtToken = authHeader.substring(7);
            }

            if (jwtToken == null) {
                return new ResponseEntity<>("Brak tokena JWT", HttpStatus.UNAUTHORIZED);
            }

            // Używamy AuthService do ustawienia hasła
            // Ta metoda sama zweryfikuje token i znajdzie użytkownika
            authService.setUserPassword(jwtToken, request.newPassword());

            return ResponseEntity.ok(Map.of("message", "Hasło ustawione pomyślnie."));

        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT); // Np. "Ma już hasło"
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST); // Np. "Za krótkie"
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /**
     * Zwraca dane (email, name) aktualnie zalogowanego użytkownika.
     */
    @GetMapping("/api/user/me")
    public ResponseEntity<?> getUserDetails(
            @RequestHeader("Authorization") String authHeader) {

        try {
            String jwtToken = extractJwtFromHeader(authHeader);
            UserDetailsResponse userDetails = authService.getUserDetails(jwtToken);
            return ResponseEntity.ok(userDetails);

        } catch (Exception e) {
            // Obsługa błędów, np. wygaśnięty token lub brak użytkownika
            return new ResponseEntity<>(Map.of("error", "Błąd pobierania danych użytkownika: " + e.getMessage()), HttpStatus.UNAUTHORIZED);
        }
    }
    /**
     * Zmienia hasło zalogowanego użytkownika.
     * Wymaga podania obecnego hasła do weryfikacji.
     */
    @PostMapping("/api/user/change-password")
    public ResponseEntity<?> changeUserPassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChangePasswordRequest request) {

        try {
            // Wyciągamy token JWT z nagłówka
            String jwtToken = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwtToken = authHeader.substring(7);
            }
            if (jwtToken == null) {
                return new ResponseEntity<>(Map.of("error", "Brak tokena JWT"), HttpStatus.UNAUTHORIZED);
            }

            // Wywołujemy nową metodę z AuthService
            authService.changeUserPassword(
                    jwtToken,
                    request.currentPassword(),
                    request.newPassword()
            );

            // Sukces
            return ResponseEntity.ok(Map.of("message", "Hasło zmienione pomyślnie."));

        } catch (SecurityException e) {
            // Z AuthService: nieprawidłowe obecne hasło
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.FORBIDDEN); // 403
        } catch (IllegalArgumentException e) {
            // Z AuthService: nowe hasło za krótkie
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.BAD_REQUEST); // 400
        } catch (IllegalStateException e) {
            // Z AuthService: użytkownik Google bez hasła
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.CONFLICT); // 409
        } catch (Exception e) {
            // Inne błędy
            return new ResponseEntity<>(Map.of("error", "Wystąpił wewnętrzny błąd serwera: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
                statusService.getUniqueAndroidIPs(),
                authorizedUsersList,
                users2FAStatusMap
        );
    }
    // ⭐️⭐️ DODAJ TĘ METODĘ POMOCNICZĄ ⭐️⭐️
    private String extractJwtFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // Rzucamy wyjątek, jeśli nagłówek jest nieprawidłowy
        throw new IllegalArgumentException("Brak lub nieprawidłowy nagłówek Authorization.");
    }
}