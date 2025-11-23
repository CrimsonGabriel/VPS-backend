package com.bazunia.vps.controller;

import com.bazunia.vps.dto.*;
import com.bazunia.vps.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<?> getUserDetails(@RequestHeader("Authorization") String authHeader) {
        try {
            String jwtToken = extractJwtFromHeader(authHeader);
            UserDetailsResponse userDetails = authService.getUserDetails(jwtToken);
            return ResponseEntity.ok(userDetails);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/set-password")
    public ResponseEntity<?> setUserPassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SetPasswordRequest request) {
        try {
            String jwtToken = extractJwtFromHeader(authHeader);
            authService.setUserPassword(jwtToken, request.newPassword());
            return ResponseEntity.ok(Map.of("message", "Hasło ustawione pomyślnie."));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changeUserPassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChangePasswordRequest request) {
        try {
            String jwtToken = extractJwtFromHeader(authHeader);
            authService.changeUserPassword(jwtToken, request.currentPassword(), request.newPassword());
            return ResponseEntity.ok(Map.of("message", "Hasło zmienione pomyślnie."));
        } catch (SecurityException e) {
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    private String extractJwtFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Brak nagłówka Authorization.");
    }
}