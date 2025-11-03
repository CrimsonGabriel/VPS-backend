package com.bazunia.vps.service;

// IMPORTY MODELU I REPOZYTORIUM
import com.bazunia.vps.model.User;
import com.bazunia.vps.repository.UserRepository;

// IMPORTY GOOGLE
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

// IMPORTY SPRINGA
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// IMPORTY JAVY
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StatusService statusService; // Teraz ma metodę addAndroidIp

    // ID Klienta z server.js
    private static final String GOOGLE_CLIENT_ID = "79063316759-iva8uesd0vlj3in6eaeralk2kdkgv5or.apps.googleusercontent.com";

    // Ta klasa zastąpi /auth/google
    public AuthResponse loginWithGoogle(String googleToken, String ipAddress) {
        try {
            // 1. Weryfikacja tokena Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                    .build();

            GoogleIdToken idToken = verifier.verify(googleToken);
            if (idToken == null) {
                throw new IllegalArgumentException("Nieprawidłowy token Google.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // 2. Znajdź lub stwórz usera (logika z server.js)
            Optional<User> userOpt = userRepository.findByEmail(email);
            User user;
            if (userOpt.isEmpty()) {
                // Nowy użytkownik
                user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setTwoFactorEnabled(false); // Domyślnie 2FA wyłączone
                user = userRepository.save(user);
            } else {
                user = userOpt.get();
            }

            // 3. Rejestracja IP (logika z server.js)
            // TERAZ TA LINIA DZIAŁA POPRAWNIE
            statusService.addAndroidIp(ipAddress);

            // 4. Sprawdzenie 2FA (logika 5 minut)
            boolean requires2FA = false;
            if (user.isTwoFactorEnabled()) {
                if (user.getLastTwoFactorLogin() == null ||
                        user.getLastTwoFactorLogin().isBefore(LocalDateTime.now().minusMinutes(5))) {
                    requires2FA = true;
                }
            }

            // 5. Zwróć odpowiedź
            if (requires2FA) {
                return new AuthResponse(null, true, "Wymagane 2FA");
            } else {
                // TODO: Wygeneruj JWT (zrobimy to później)
                String jwt = "dummy-jwt-token"; // Placeholder
                return new AuthResponse(jwt, false, "Zalogowano");
            }

        } catch (Exception e) {
            throw new RuntimeException("Błąd logowania Google: " + e.getMessage());
        }
    }

    // Prosta klasa do trzymania odpowiedzi
    public record AuthResponse(String jwt, boolean requires2FA, String message) {}
}