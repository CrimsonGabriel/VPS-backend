package com.bazunia.vps.controller;

import com.bazunia.vps.dto.*;
import com.bazunia.vps.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final Logger controllerLogger = Logger.getLogger(AuthController.class.getName());

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // --- Funkcja pomocnicza dla IP ---
    private String getClientIp(HttpServletRequest request) {
        String forwardedIp = request.getHeader("X-Forwarded-For");
        if (forwardedIp == null || forwardedIp.isEmpty()) {
            return request.getRemoteAddr();
        }
        return forwardedIp.split(",")[0].trim();
    }

    // --- Metoda pomocnicza do wyciągania tokena (z nagłówka lub ciała) ---
    private String extractGoogleToken(String authHeader, Map<String, String> body) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        } else if (body != null && body.containsKey("token")) {
            return body.get("token");
        }
        return null;
    }

    // --- ENDPOINT LOGOWANIA GOOGLE (Android) - Pełna ścieżka: /api/auth/google ---
    @PostMapping("/google")
    public ResponseEntity<AuthService.AuthResponse> authGoogle(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        controllerLogger.info(">>> OTRZYMANO ŻĄDANIE /api/auth/google z IP: " + ip);

        String token = extractGoogleToken(authHeader, body);
        if (token == null) {
            controllerLogger.warning("Brak tokena w żądaniu /api/auth/google.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthService.AuthResponse(null, false, "Brak tokena", false));
        }

        try {
            AuthService.AuthResponse response = authService.loginWithGoogle(token, ip);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            controllerLogger.warning("Błąd w loginWithGoogle: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthService.AuthResponse(null, false, e.getMessage(), false));
        }
    }

    // --- ENDPOINT REJESTRACJI REACT - Pełna ścieżka: /api/auth/register ---
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest registerRequest) {
        try {
            authService.registerUser(registerRequest);
            return ResponseEntity.ok("Użytkownik zarejestrowany pomyślnie");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- ENDPOINT LOGOWANIA REACT - Pełna ścieżka: /api/auth/login ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            String ip = getClientIp(request);
            // ⭐️ POPRAWKA: Zmieniono typ z 'LoginResponse' na 'AuthService.AuthResponse'
            AuthService.AuthResponse response = authService.loginUser(loginRequest, ip);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Nieprawidłowy login lub hasło."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ⭐️⭐️ NOWY ENDPOINT: REJESTRACJA ANDROID ⭐️⭐️
    // Pełna ścieżka: /api/auth/android/register
    @PostMapping("/android/register")
    public ResponseEntity<?> registerAndroidUser(@RequestBody EmailRegistrationRequest request) {
        try {
            authService.registerAndroidUser(request);
            // 200 OK oznacza, że serwer przyjął żądanie i wysyła e-mail
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            // Jeśli e-mail jest zajęty (z AuthService)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Błąd serwera podczas rejestracji");
        }
    }

    // ⭐️⭐️ NOWY ENDPOINT: AKTYWACJA KONTA ANDROID ⭐️⭐️
    // Pełna ścieżka: /api/auth/android/activate
    @GetMapping("/android/activate")
    public ResponseEntity<String> activateAccount(@RequestParam("token") String token) {
        try {
            authService.activateUser(token);
            // Zwraca prostą stronę HTML z informacją o sukcesie
            return ResponseEntity.ok("<h1>Twoje konto zostało pomyślnie aktywowane!</h1><p>Możesz teraz zalogować się w aplikacji.</p>");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("<h1>Błąd</h1><p>Nieprawidłowy lub wygasły link aktywacyjny.</p>");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("<h1>Błąd</h1><p>Wystąpił wewnętrzny błąd serwera.</p>");
        }
    }


    // --- ENDPOINTY 2FA ---
    // Pełna ścieżka: /api/auth/2fa/status
    @GetMapping("/2fa/status")
    public ResponseEntity<?> getTwoFaStatus(@RequestHeader("Authorization") String authHeader) {
        String token = extractGoogleToken(authHeader, null);
        if (token == null) return new ResponseEntity<>("Brak tokena", HttpStatus.UNAUTHORIZED);

        try {
            boolean isEnabled = authService.getTwoFaStatus(token);
            return ResponseEntity.ok(new TwoFaStatusResponse(isEnabled));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    // Pełna ścieżka: /api/auth/2fa/login-verify
    @PostMapping("/2fa/login-verify")
    public ResponseEntity<?> loginVerifyTwoFa(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TwoFaRequest body) {

        String token = extractGoogleToken(authHeader, null);
        if (token == null) return new ResponseEntity<>("Brak tokena", HttpStatus.UNAUTHORIZED);

        try {
            LoginResponse response = authService.loginVerifyTwoFa(token, body.totpCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    // Pełna ścieżka: /api/auth/2fa/setup
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, path = "/2fa/setup")
    public ResponseEntity<?> setupTwoFa(@RequestHeader("Authorization") String authHeader) {
        String token = extractGoogleToken(authHeader, null);
        if (token == null) return new ResponseEntity<>("Brak tokena", HttpStatus.UNAUTHORIZED);

        try {
            TwoFaSetupResponse response = authService.setupTwoFa(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Pełna ścieżka: /api/auth/2fa/verify
    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verifyTwoFa(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TwoFaRequest body) {

        String token = extractGoogleToken(authHeader, null);
        if (token == null) return new ResponseEntity<>("Brak tokena", HttpStatus.UNAUTHORIZED);

        try {
            boolean success = authService.verifyAndEnableTwoFa(token, body.totpCode());
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "2FA włączone."));
            } else {
                return ResponseEntity.status(400).body(Map.of("success", false, "error", "Nieprawidłowy kod 2FA."));
            }
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Pełna ścieżka: /api/auth/2fa/disable
    @PostMapping("/2fa/disable")
    public ResponseEntity<?> disableTwoFa(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TwoFaRequest body) {

        String token = extractGoogleToken(authHeader, null);
        if (token == null) return new ResponseEntity<>("Brak tokena", HttpStatus.UNAUTHORIZED);

        try {
            boolean success = authService.disableTwoFa(token, body.totpCode());
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "2FA wyłączone."));
            } else {
                return ResponseEntity.status(400).body(Map.of("success", false, "error", "Nieprawidłowy kod 2FA."));
            }
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    @PostMapping("/2fa/email-verify")
    public ResponseEntity<?> loginVerifyEmailTwoFa(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TwoFaRequest body) {

        String token = extractGoogleToken(authHeader, null); // Twoja metoda działa też na JWT
        if (token == null) return new ResponseEntity<>("Brak tokena", HttpStatus.UNAUTHORIZED);

        try {
            LoginResponse response = authService.loginVerifyEmailTwoFa(token, body.totpCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

}