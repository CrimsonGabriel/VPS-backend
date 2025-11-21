// Plik: com/bazunia/vps/controller/ApiController.java
package com.bazunia.vps.controller;

import com.bazunia.vps.dto.UpdateDtos;
import com.bazunia.vps.model.User;
import com.bazunia.vps.model.SensorReading;
import com.bazunia.vps.model.Gateway;
import com.bazunia.vps.model.Sensor;
import com.bazunia.vps.model.SystemSetting;
import com.bazunia.vps.model.UserGatewayPermission;
import com.bazunia.vps.model.UserGatewayPermissionId;
import com.bazunia.vps.dto.SensorDto;
import com.bazunia.vps.dto.TwoFaStatusDto;
import com.bazunia.vps.dto.RegistrationRequest;
import com.bazunia.vps.dto.StatusResponse;
import com.bazunia.vps.dto.UpdateRequest;
import com.bazunia.vps.dto.SetPasswordRequest;
import com.bazunia.vps.dto.ChangePasswordRequest;
import com.bazunia.vps.dto.UserDetailsResponse;
import com.bazunia.vps.dto.BatteryUpdateRequest;
import com.bazunia.vps.dto.SimpleDataRequest;
import com.bazunia.vps.dto.GatewayUpdateRequest;
import com.bazunia.vps.dto.SensorReadingResponseDto;
import com.bazunia.vps.dto.GatewayDto;
import com.bazunia.vps.dto.SensorUpdateRequest;
import com.bazunia.vps.dto.SensorConfigDto;
import com.bazunia.vps.dto.PasswordRequest;
import com.bazunia.vps.dto.SensorStatusErrorDto;
import com.bazunia.vps.dto.RiskReportDto;
import com.bazunia.vps.dto.ShareDto;


import com.bazunia.vps.repository.SensorReadingRepository;
import com.bazunia.vps.repository.UserRepository;
import com.bazunia.vps.repository.GatewayRepository;
import com.bazunia.vps.repository.SensorRepository;
import com.bazunia.vps.repository.SystemSettingRepository;
import com.bazunia.vps.repository.UserGatewayPermissionRepository;


import com.bazunia.vps.service.DataService;
import com.bazunia.vps.service.StatusService;
import com.bazunia.vps.service.UpdateService;
import com.bazunia.vps.service.AuthService;
import com.bazunia.vps.service.JwtService;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

@RestController
public class ApiController {

    private static final String SECRET_PASSWORD = "ZMIEN_TO_HASLO_XD";

    private final StatusService statusService;
    private final SensorReadingRepository sensorReadingRepository;
    private final UserRepository userRepository;
    private final DataService dataService;

    private final GatewayRepository gatewayRepository;
    private final SensorRepository sensorRepository;
    private final SystemSettingRepository systemSettingRepository;

    private final UpdateService updateService;
    private final JwtService jwtService;
    private final AuthService authService;
    private final UserGatewayPermissionRepository permissionRepository;

    @Autowired
    public ApiController(StatusService statusService,
                         SensorReadingRepository sensorReadingRepository,
                         UserRepository userRepository,
                         DataService dataService,
                         UpdateService updateService,
                         JwtService jwtService, AuthService authService,
                         GatewayRepository gatewayRepository,
                         SensorRepository sensorRepository,
                         SystemSettingRepository systemSettingRepository,
                         UserGatewayPermissionRepository permissionRepository) {
        this.statusService = statusService;
        this.sensorReadingRepository = sensorReadingRepository;
        this.userRepository = userRepository;
        this.dataService = dataService;
        this.updateService = updateService;
        this.jwtService = jwtService;
        this.authService = authService;
        this.gatewayRepository = gatewayRepository;
        this.sensorRepository = sensorRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.permissionRepository = permissionRepository;
    }

    @GetMapping("/api/sensors/status")
    public ResponseEntity<List<SensorStatusErrorDto>> getSensorStatuses(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<SensorStatusErrorDto> errors = dataService.checkSensorStatuses(user);
        return ResponseEntity.ok(errors);
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

    /**
     * Nowy endpoint do masowej aktualizacji stanu baterii czujników.
     * Chroniony przez SECRET_PASSWORD, tak jak /data.
     */
    @PostMapping("/api/sensors/battery")
    @Transactional // Ważne: zapewnia, że wszystkie aktualizacje albo przejdą, albo żadna
    public ResponseEntity<String> updateBatteryStatuses(
            @RequestBody BatteryUpdateRequest request) {

        // 1. Sprawdź hasło
        if (!SECRET_PASSWORD.equals(request.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }

        if (request.statuses() == null || request.statuses().isEmpty()) {
            return new ResponseEntity<>("Brak statusów do aktualizacji", HttpStatus.BAD_REQUEST);
        }

        int updatedCount = 0;
        int notFoundCount = 0;

        // 2. Iteruj i aktualizuj
        for (var statusDto : request.statuses()) {
            if (statusDto.sensorId() == null || statusDto.level() == null) {
                continue; // Pomiń niekompletne dane
            }

            // Użyj repozytorium sensorRepository, które już masz wstrzyknięte
            var sensorOpt = sensorRepository.findById(statusDto.sensorId());

            if (sensorOpt.isPresent()) {
                Sensor sensor = sensorOpt.get();
                sensor.setBatteryLevel(statusDto.level());
                sensorRepository.save(sensor); // Zapisz zmiany
                updatedCount++;
            } else {
                notFoundCount++;
            }
        }

        String message = "Zaktualizowano baterię dla " + updatedCount + " czujników.";
        if (notFoundCount > 0) {
            message += " Nie znaleziono " + notFoundCount + " czujników.";
        }

        return ResponseEntity.ok(message);
    }

    /**
     * Zwraca konfigurację (ID, typ, interwał) dla wszystkich czujników.
     * RPi używa tego do dynamicznego ustawiania pętli odpytywania.
     * Chronione tym samym hasłem co /data.
     */
    @PostMapping("/api/sensors/config")
    public ResponseEntity<?> getSensorConfig(
            @RequestBody PasswordRequest request) {

        // 1. Sprawdź hasło
        if (!SECRET_PASSWORD.equals(request.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }

        try {
            // 2. Pobierz konfigurację z serwisu
            List<SensorConfigDto> configList = dataService.getAllSensorConfigs();
            // Zwracamy listę jako główny obiekt JSON
            return ResponseEntity.ok(configList);

        } catch (Exception e) {
            return new ResponseEntity<>("Błąd serwera podczas pobierania konfiguracji: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    // ⭐️⭐️⭐️ NOWE ENDPOINTY: WNIOSKOWANIE O RETENCJĘ DANYCH ⭐️⭐️⭐️

    /**
     * Android wysyła propozycję zmiany ustawień retencji.
     * Zapisujemy to jako "wniosek" (PENDING).
     */
    @PostMapping("/api/retention/request")
    public ResponseEntity<?> requestRetentionChange(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        // Możemy logować, kto poprosił (User user = ...), ale tutaj zapiszemy po prostu wniosek
        String days = request.get("days");
        String size = request.get("size");

        if (days == null && size == null) {
            return ResponseEntity.badRequest().body("Musisz podać dni lub rozmiar.");
        }

        // Zapisz wniosek w bazie (używamy specjalnych kluczy dla wniosków)
        if (days != null) systemSettingRepository.save(new SystemSetting("RETENTION_REQ_DAYS", days));
        if (size != null) systemSettingRepository.save(new SystemSetting("RETENTION_REQ_SIZE", size));

        // Ustaw status na PENDING (Oczekuje na admina)
        systemSettingRepository.save(new SystemSetting("RETENTION_REQ_STATUS", "PENDING"));

        return ResponseEntity.ok(Map.of("message", "Wniosek wysłany do administratora via Frontend."));
    }

    /**
     * Android sprawdza status swojego wniosku (polling).
     * Zwraca też OBECNE ustawienia, żeby wyświetlić je w UI.
     */
    @GetMapping("/api/retention/status")
    public ResponseEntity<?> getRetentionRequestStatus(Authentication authentication) {

        // Pobierz status wniosku
        String status = systemSettingRepository.findById("RETENTION_REQ_STATUS")
                .map(SystemSetting::getValue)
                .orElse("NONE");

        // Pobierz aktualnie obowiązujące ustawienia (żeby wyświetlić w Androidzie)
        String currentDays = systemSettingRepository.findById("RETENTION_DAYS")
                .map(SystemSetting::getValue).orElse("0");
        String currentSize = systemSettingRepository.findById("RETENTION_MAX_RECORDS")
                .map(SystemSetting::getValue).orElse("0");

        return ResponseEntity.ok(Map.of(
                "status", status,
                "currentDays", currentDays,
                "currentSize", currentSize
        ));
    }

    // ⭐️ ⭐️ ⭐️ NOWY ENDPOINT (PRZENIESIONY Z ADMINA) ⭐️ ⭐️ ⭐️
    /**
     * Ustawia globalny interwał odpytywania dla WSZYSTKICH czujników.
     * Dostępne dla każdego zalogowanego użytkownika.
     */
    @PostMapping("/api/sensors/interval/global")
    public ResponseEntity<?> setGlobalSensorInterval(@RequestBody Map<String, Integer> request) {
        Integer interval = request.get("interval");
        if (interval == null) {
            return new ResponseEntity<>("Brak parametru 'interval'", HttpStatus.BAD_REQUEST);
        }

        try {
            // Używamy dataService, które jest już dostępne w tej klasie
            int updatedCount = dataService.setGlobalSensorInterval(interval);
            return ResponseEntity.ok(Map.of(
                    "message", "Zaktualizowano interwał dla " + updatedCount + " czujników.",
                    "updatedCount", updatedCount,
                    "newInterval", interval
            ));
        } catch (Exception e) {
            return new ResponseEntity<>("Błąd serwera: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

// --- NOWE ENDPOINTY DLA AKTUALIZACJI (ANDROID) ---

    @GetMapping("/api/my-updates")
    public ResponseEntity<List<UpdateDtos.ClientUpdateResponse>> getMyUpdates(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(updateService.getPendingUpdatesForUser(user.getId()));
    }

    @PostMapping("/api/updates/{assignmentId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long assignmentId,
            @RequestBody UpdateDtos.UpdateStatusRequest request) {
        try {
            updateService.updateAssignmentStatus(assignmentId, request.getStatus());
            return ResponseEntity.ok(Map.of("message", "Status updated to " + request.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // ⭐️⭐️⭐️ NOWY ENDPOINT: RAPORT BEZPIECZEŃSTWA ⭐️⭐️⭐️
    @GetMapping("/api/status/risk-report")
    public ResponseEntity<RiskReportDto> getRiskReport(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        RiskReportDto report = dataService.generateRiskReport(user);
        return ResponseEntity.ok(report);
    }


    // --- ENDPOINT DLA RASPBERRY PI: DANE (/data) ---
// <<< ZMIANA: Cała metoda przepisana, aby używać SimpleDataRequest
    @PostMapping("/data")
    public ResponseEntity<String> postData(@RequestBody SimpleDataRequest data, HttpServletRequest request) {
        if (!SECRET_PASSWORD.equals(data.password())) {
            return new ResponseEntity<>("Nieprawidłowe hasło", HttpStatus.UNAUTHORIZED);
        }
        String clientIp = request.getRemoteAddr();
        statusService.updateRpiIp(clientIp);
        statusService.addRpiIp(clientIp);

        try {
            // Iterujemy po nowym, prostym DTO
            for (var sensorDto : data.sensors()) {

                // Pobieramy ID z płaskiej struktury
                Long gatewayId = Long.parseLong(sensorDto.gateway_id());
                Long sensorId = Long.parseLong(sensorDto.sensor_id());

                // 1. Znajdź bramkę
                Gateway gateway = gatewayRepository.findById(gatewayId)
                        .orElseThrow(() -> new RuntimeException("Nie znaleziono bramki o ID: " + gatewayId));

                // 2. Oznacz bramkę jako "online"
                gateway.setStatus("online");
                gateway.setLastSeen(java.time.LocalDateTime.now());
                gatewayRepository.save(gateway); // Zapisujemy zaktualizowany status

                // 3. Znajdź czujnik
                Sensor sensor = sensorRepository.findById(sensorId)
                        .orElseThrow(() -> new RuntimeException("Nie znaleziono czujnika o ID: " + sensorId));

                // 4. Stwórz i zapisz odczyt (teraz to trafi do sensor_reading)
                SensorReading newReading = new SensorReading();
                newReading.setGateway(gateway);
                newReading.setSensor(sensor);
                newReading.setValue(sensorDto.value());
                newReading.setTimestamp(sensorDto.timestamp());

                sensorReadingRepository.save(newReading);
            }
        } catch (NumberFormatException e) {
            return new ResponseEntity<>("Błąd przetwarzania danych: Nieprawidłowy format ID (nie jest liczbą). " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            // Ten błąd (np. "Nie znaleziono bramki") jest teraz jedynym powodem błędu 400
            return new ResponseEntity<>("Błąd przetwarzania danych: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

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
    public ResponseEntity<Map<String, List<SensorReadingResponseDto>>> getSensorData() {

        // <<< POPRAWKA: Tworzymy obiekt Pageable, aby pobrać "Top 10" >>>
        Pageable topTen = PageRequest.of(0, 10); // Strona 0, rozmiar 10

        // <<< POPRAWKA: Wywołujemy nową, poprawną metodę z repozytorium >>>
        List<SensorReadingResponseDto> readings = sensorReadingRepository.findLatestReadingsWithDetails(topTen);

        Map<String, List<SensorReadingResponseDto>> response = Map.of("sensors", readings);
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
    private User getAuthenticatedUser(Authentication authentication) {
        // Spring Security Principal jest już pełnym obiektem User pobranym przez AuthService
        return (User) authentication.getPrincipal();
    }

    // === NOWE ENDPOINTY DO ZARZĄDZANIA BRAMKAMI (WYMAGANIA 2.x) ===

    @GetMapping("/api/gateways")
    public ResponseEntity<List<GatewayDto>> getGateways(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<GatewayDto> gateways = dataService.getGatewaysForUser(user);
        return ResponseEntity.ok(gateways);
    }

    @PutMapping("/api/gateways/{id}")
    public ResponseEntity<GatewayDto> updateGateway( // ZMIANA: Zwracamy GatewayDto
                                                     @PathVariable Long id,
                                                     @RequestBody GatewayUpdateRequest request,
                                                     Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        try {
            Gateway updatedGateway = dataService.updateGateway(id, request, user);
            // ZMIANA: Mapujemy na DTO przed wysłaniem
            return ResponseEntity.ok(GatewayDto.fromEntity(updatedGateway));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PutMapping("/api/sensors/{id}")
    public ResponseEntity<SensorDto> updateSensor( // ZMIANA: Zwracamy SensorDto
                                                   @PathVariable Long id,
                                                   @RequestBody SensorUpdateRequest request,
                                                   Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        try {
            Sensor updatedSensor = dataService.updateSensor(id, request, user);
            // ZMIANA: Mapujemy na DTO
            return ResponseEntity.ok(SensorDto.fromEntity(updatedSensor));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/api/sensors/{id}/toggle-reporting")
    public ResponseEntity<SensorDto> toggleSensorReporting( // ZMIANA: Zwracamy SensorDto
                                                            @PathVariable Long id,
                                                            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        try {
            Sensor updatedSensor = dataService.toggleSensorReporting(id, user);
            // ZMIANA: Mapujemy na DTO
            return ResponseEntity.ok(SensorDto.fromEntity(updatedSensor));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/api/gateways/{id}")
    public ResponseEntity<Void> deleteGateway(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        try {
            // <<< ZMIANA WYWOŁANIA: z deleteGateway na disassociateGateway >>>
            dataService.disassociateGateway(id, user);

            return ResponseEntity.noContent().build(); // Sukces, 204 No Content
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // ============================================================================
    // ===                    SEKCJA UDOSTĘPNIANIA BRAMEK (SHARING)             ===
    // ============================================================================

    /**
     * 1. Pobierz listę użytkowników, którym MOŻNA udostępnić bramkę.
     * Filtruje:
     * - Samego siebie (nie udostępniamy sobie)
     * - Adminów (zakładamy, że admin ma dostęp do wszystkiego lub nie chcemy mu śmiecić)
     */
    @GetMapping("/api/users/available-for-share")
    public ResponseEntity<List<ShareDto.UserPickDto>> getUsersForShare(Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        List<ShareDto.UserPickDto> availableUsers = userRepository.findAll().stream()
                // Wyklucz samego siebie
                .filter(u -> !u.getId().equals(currentUser.getId()))
                // Wyklucz adminów (zakładam, że rola to Enum lub String, dostosuj warunek jeśli masz inaczej)
                .filter(u -> u.getRole() != null && "USER".equals(u.getRole().toString()))
                .map(u -> new ShareDto.UserPickDto(u.getId(), u.getEmail()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(availableUsers);
    }

    /**
     * 2. Pobierz listę osób, którym TA bramka jest już udostępniona.
     */
    @GetMapping("/api/gateways/{gatewayId}/shares")
    public ResponseEntity<?> getGatewayShares(@PathVariable Long gatewayId, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Gateway not found"));

        // Tylko właściciel może widzieć, komu udostępnił
        if (!gateway.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only owner can view shares.");
        }

        List<ShareDto.SharedUserDto> shares = permissionRepository.findByGatewayId(gatewayId).stream()
                .map(perm -> new ShareDto.SharedUserDto(
                        perm.getUser().getId(),
                        perm.getUser().getEmail(),
                        perm.getPermissionLevel()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(shares);
    }

    /**
     * 3. Udostępnij bramkę (Dodaj lub Zaktualizuj uprawnienie).
     */
    @PostMapping("/api/gateways/share")
    public ResponseEntity<?> shareGateway(@RequestBody ShareDto.ShareGatewayRequest request, Authentication authentication) {
        // --- DEBUG START ---
        System.out.println("--- DEBUG: Próba udostępnienia bramki ---");
        if (authentication == null) {
            System.out.println("DEBUG: Authentication is NULL!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No authentication");
        }
        User currentUser = getAuthenticatedUser(authentication);
        System.out.println("DEBUG: User ID: " + currentUser.getId() + ", Email: " + currentUser.getEmail());
        System.out.println("DEBUG: Request GatewayID: " + request.getGatewayId());
        System.out.println("DEBUG: Request TargetUserID: " + request.getTargetUserId());
        // --- DEBUG END ---

        Gateway gateway = gatewayRepository.findById(request.getGatewayId())
                .orElseThrow(() -> new EntityNotFoundException("Gateway not found"));

        // 1. Sprawdź czy jesteś właścicielem
        if (!gateway.getOwner().getId().equals(currentUser.getId())) {
            System.out.println("DEBUG: Brak uprawnień właściciela. OwnerID=" + gateway.getOwner().getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only owner can share gateway.");
        }

        // 2. Znajdź usera docelowego
        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new EntityNotFoundException("Target user not found"));

        // 3. Stwórz lub zaktualizuj uprawnienie
        UserGatewayPermissionId permId = new UserGatewayPermissionId(targetUser.getId(), gateway.getId());

        UserGatewayPermission permission = new UserGatewayPermission();
        permission.setId(permId);
        permission.setUser(targetUser);
        permission.setGateway(gateway);
        permission.setPermissionLevel(request.getPermissionLevel()); // VIEW lub FULL_ACCESS

        permissionRepository.save(permission);

        System.out.println("DEBUG: Sukces. Udostępniono.");
        return ResponseEntity.ok(Map.of("message", "Gateway shared successfully with " + targetUser.getEmail()));
    }

    /**
     * 4. Usuń udostępnienie (Odbierz dostęp).
     */
    @DeleteMapping("/api/gateways/{gatewayId}/share/{userId}")
    public ResponseEntity<?> removeShare(
            @PathVariable Long gatewayId,
            @PathVariable Long userId,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Gateway not found"));

        // Tylko właściciel może odbierać uprawnienia
        if (!gateway.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only owner can remove shares.");
        }

        Optional<UserGatewayPermission> permission = permissionRepository.findByGatewayIdAndUserId(gatewayId, userId);

        if (permission.isPresent()) {
            permissionRepository.delete(permission.get());
            return ResponseEntity.ok(Map.of("message", "Access removed."));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}