package com.bazunia.vps.controller;

import com.bazunia.vps.dto.*;
import com.bazunia.vps.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<AuthService.AuthResponse> authGoogle(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {

        String token = extractToken(authHeader, body);
        if (token == null) {
            throw new IllegalArgumentException("Brak tokena Google.");
        }

        String ip = getClientIp(request);
        log.info("Logowanie Google z IP: {}", ip);

        return ResponseEntity.ok(authService.loginWithGoogle(token, ip));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthService.AuthResponse> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        return ResponseEntity.ok(authService.loginUser(loginRequest, ip));
    }

    @PostMapping("/register")
    public ResponseEntity<String> register() {
        throw new UnsupportedOperationException("Ten endpoint jest wyłączony. Użyj /api/auth/android/register");
    }

    @PostMapping("/android/register")
    public ResponseEntity<Void> registerAndroidUser(@RequestBody EmailRegistrationRequest request) {
        authService.registerAndroidUser(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/android/activate")
    public ResponseEntity<String> activateAccount(@RequestParam("token") String token) {
        authService.activateUser(token);
        return ResponseEntity.ok("<h1>Konto aktywowane!</h1><p>Możesz się zalogować.</p>");
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<?> requestPasswordReset(@RequestBody RequestPasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok(Map.of("message", "Jeśli email istnieje, wysłano link."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Hasło zresetowane."));
    }

    @GetMapping("/2fa/status")
    public ResponseEntity<TwoFaStatusResponse> getTwoFaStatus(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader, null);
        boolean isEnabled = authService.getTwoFaStatus(token);
        return ResponseEntity.ok(new TwoFaStatusResponse(isEnabled));
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, path = "/2fa/setup")
    public ResponseEntity<TwoFaSetupResponse> setupTwoFa(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader, null);
        return ResponseEntity.ok(authService.setupTwoFa(token));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verifyTwoFa(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TwoFaRequest body) {

        String token = extractToken(authHeader, null);
        if (authService.verifyAndEnableTwoFa(token, body.totpCode())) {
            return ResponseEntity.ok(Map.of("success", true, "message", "2FA włączone."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Błędny kod."));
        }
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<?> disableTwoFa(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TwoFaRequest body) {

        String token = extractToken(authHeader, null);
        if (authService.disableTwoFa(token, body.totpCode())) {
            return ResponseEntity.ok(Map.of("success", true, "message", "2FA wyłączone."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Błędny kod."));
        }
    }

    @PostMapping("/2fa/login-verify")
    public ResponseEntity<LoginResponse> loginVerifyTwoFa(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TwoFaRequest body) {

        String token = extractToken(authHeader, null);
        return ResponseEntity.ok(authService.loginVerifyTwoFa(token, body.totpCode()));
    }

    @PostMapping("/2fa/email-verify")
    public ResponseEntity<LoginResponse> loginVerifyEmailTwoFa(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TwoFaRequest body) {

        String token = extractToken(authHeader, null);
        return ResponseEntity.ok(authService.loginVerifyEmailTwoFa(token, body.totpCode()));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedIp = request.getHeader("X-Forwarded-For");
        if (forwardedIp == null || forwardedIp.isEmpty()) {
            return request.getRemoteAddr();
        }
        return forwardedIp.split(",")[0].trim();
    }

    private String extractToken(String authHeader, Map<String, String> body) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        } else if (body != null && body.containsKey("token")) {
            return body.get("token");
        }
        return null;
    }
}