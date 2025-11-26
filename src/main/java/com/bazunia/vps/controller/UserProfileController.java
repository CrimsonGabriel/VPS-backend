// 💾 src/main/java/com/bazunia/vps/controller/UserProfileController.java

package com.bazunia.vps.controller;

import com.bazunia.vps.dto.ChangePasswordRequest;
import com.bazunia.vps.dto.SetPasswordRequest;
import com.bazunia.vps.dto.UpdateProfileRequest;
import com.bazunia.vps.dto.UserDetailsResponse;
import com.bazunia.vps.model.FileRecord;
import com.bazunia.vps.model.User;
import com.bazunia.vps.repository.UserRepository;
import com.bazunia.vps.service.AuthService;
import com.bazunia.vps.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final AuthService authService;
    private final UserRepository userRepository;     // Potrzebne do zapisu User
    private final FileStorageService fileStorageService; // Potrzebne do zapisu Avatara

    // --- POBIERANIE DANYCH ---
    @GetMapping("/me")
    public ResponseEntity<?> getUserDetails(@RequestHeader("Authorization") String authHeader) {
        try {
            String email = extractEmailFromToken(authHeader);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Nie znaleziono użytkownika"));

            // Budujemy odpowiedź ręcznie lub przez mapper
            UserDetailsResponse response = new UserDetailsResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getRole().name(),
                    user.isTwoFactorEnabled(),
                    user.getAvatarUrl()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.UNAUTHORIZED);
        }
    }

    // --- AKTUALIZACJA DANYCH (Np. Imię) ---
    @PutMapping("/me")
    public ResponseEntity<?> updateUserProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateProfileRequest request) {
        try {
            String email = extractEmailFromToken(authHeader);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Nie znaleziono użytkownika"));

            if (request.name() != null && !request.name().isBlank()) {
                user.setName(request.name());
            }

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Profil zaktualizowany pomyślnie."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Błąd aktualizacji profilu"));
        }
    }

    // --- UPLOAD AVATARA ---
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file) {
        try {
            String email = extractEmailFromToken(authHeader);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Nie znaleziono użytkownika"));

            // 1. Zapisz plik fizycznie (używając istniejącego serwisu)
            FileRecord savedFile = fileStorageService.storeFile(file);

            // 2. Wygeneruj URL do pobrania tego pliku
            // Zakładamy, że FileController obsługuje /api/files/download/{id}
            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/files/download/")
                    .path(savedFile.getId().toString())
                    .toUriString();

            // 3. Zaktualizuj użytkownika
            user.setAvatarUrl(fileDownloadUri);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Avatar zaktualizowany.",
                    "avatarUrl", fileDownloadUri
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Nie udało się wgrać avatara: " + e.getMessage()));
        }
    }

    // --- ZMIANA HASŁA (Bez zmian logicznych) ---
    @PostMapping("/change-password")
    public ResponseEntity<?> changeUserPassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChangePasswordRequest request) {
        try {
            // Uwaga: AuthService.changeUserPassword wymaga tokena (String), a nie emaila,
            // więc tutaj przekazujemy czysty token bez "Bearer "
            String token = extractTokenRaw(authHeader);
            authService.changeUserPassword(token, request.currentPassword(), request.newPassword());
            return ResponseEntity.ok(Map.of("message", "Hasło zmienione pomyślnie."));
        } catch (SecurityException e) {
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/set-password")
    public ResponseEntity<?> setUserPassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SetPasswordRequest request) {
        try {
            String token = extractTokenRaw(authHeader);
            authService.setUserPassword(token, request.newPassword());
            return ResponseEntity.ok(Map.of("message", "Hasło ustawione pomyślnie."));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // --- HELPERY ---
    private String extractTokenRaw(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Brak nagłówka Authorization.");
    }

    private String extractEmailFromToken(String authHeader) {
        String token = extractTokenRaw(authHeader);
        // Pobieramy email z tokena za pomocą metody w AuthService lub JwtService
        // Tutaj zakładam, że AuthService ma metodę pomocniczą lub użyjemy JwtService bezpośrednio.
        // Dla uproszczenia wywołamy extractUsername z JwtService (musisz wstrzyknąć JwtService jeśli AuthService tego nie eksponuje)
        // Ale bezpieczniej: AuthService zazwyczaj ma metodę getUserDetails(token).

        return authService.getUserDetails(token).email();
    }
}