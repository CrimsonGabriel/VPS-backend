package com.bazunia.vps.service;
import java.util.logging.Logger;
import com.bazunia.vps.dto.LoginRequest;
import com.bazunia.vps.dto.LoginResponse;
import com.bazunia.vps.dto.RegisterRequest;
import com.bazunia.vps.dto.TwoFaSetupResponse;
import com.bazunia.vps.model.User;
import com.bazunia.vps.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Importy dla 2FA
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.SecretGenerator;

import java.time.LocalDateTime;
import java.util.Collections;


@Service
public class AuthService {

    // Stare zależności
    private final UserRepository userRepository;
    private final StatusService statusService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    // NOWE zależności dla 2FA
    private final SecretGenerator secretGenerator;
    private final CodeVerifier codeVerifier;

    // KLUCZ SERWERA DO WERYFIKACJI
    private static final String GOOGLE_CLIENT_ID = "79063316759-iva8uesd0vlj3in6eaeralk2kdkgv5or.apps.googleusercontent.com";
    private static final Logger logger = Logger.getLogger(AuthService.class.getName());

    @Autowired
    public AuthService(UserRepository userRepository, StatusService statusService, JwtService jwtService,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       SecretGenerator secretGenerator, CodeVerifier codeVerifier) { // Dodane 2FA
        this.userRepository = userRepository;
        this.statusService = statusService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.secretGenerator = secretGenerator;
        this.codeVerifier = codeVerifier;
    }

    // --- Metoda pomocnicza do weryfikacji Google (Z DODANYMI LOGAMI) ---
    private User verifyGoogleTokenAndGetUser(String googleToken) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                .build();

        GoogleIdToken idToken = verifier.verify(googleToken);
        if (idToken == null) {
            // ⭐️ DODANY LOG DIAGNOSTYCZNY ⭐️
            logger.warning("Niepowodzenie weryfikacji tokena Google. Token ID jest NULL.");
            throw new IllegalArgumentException("Nieprawidłowy token Google.");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        // ⭐️ DODANY LOG DIAGNOSTYCZNY ⭐️
        logger.info("Pomyślna weryfikacja Google dla email: " + email);

        // Znajdź lub stwórz usera
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setTwoFactorEnabled(false);
            logger.info("Tworzenie nowego usera: " + email);
            return userRepository.save(newUser);
        });
    }

    // --- LOGIKA DLA ANDROIDA (Google Auth) ---

    public AuthResponse loginWithGoogle(String googleToken, String ipAddress) {
        try {
            User user = verifyGoogleTokenAndGetUser(googleToken);
            statusService.addAndroidIp(ipAddress); // <-- TO BYŁ JEDYNY WIDOCZNY LOG

            // Logika 5 minut (z server.js)
            boolean requires2FA = false;
            if (user.isTwoFactorEnabled()) {
                if (user.getLastTwoFactorLogin() == null ||
                        user.getLastTwoFactorLogin().isBefore(LocalDateTime.now().minusMinutes(5))) {
                    requires2FA = true;
                }
            }

            if (requires2FA) {
                // Zwraca null JWT, ale idToken jest używany w kolejnym kroku
                return new AuthResponse(null, true, "Wymagane 2FA");
            } else {
                String jwt = jwtService.generateToken(user);
                // Zwraca JWT, które Android powinien zapisać i użyć
                return new AuthResponse(jwt, false, "Zalogowano przez Google");
            }
        } catch (Exception e) {
            // W przypadku błędu weryfikacji (np. "Nieprawidłowy token Google"), rzuca runtime exception
            throw new RuntimeException("Błąd logowania Google: " + e.getMessage());
        }
    }

    // --- LOGIKA DLA REACTA (Login/Pass) ---

    public void registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("Email już zajęty");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }

    public LoginResponse loginUser(LoginRequest request, String ipAddress) { // <-- 1. Dodaj ipAddress
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userRepository.findByEmail(request.email()).orElseThrow();

        // 2. Dodaj IP do serwisu statusu (tak jak robi to logowanie Google)
        statusService.addAndroidIp(ipAddress);

        String jwt = jwtService.generateToken(user);
        return new LoginResponse(jwt);
    }

    // === ⭐️ NOWA LOGIKA DLA ENDPOINTÓW 2FA (Przeniesiona z server.js) ⭐️ ===

    public boolean getTwoFaStatus(String googleToken) throws Exception {
        User user = verifyGoogleTokenAndGetUser(googleToken);
        return user.isTwoFactorEnabled();
    }

    public TwoFaSetupResponse setupTwoFa(String googleToken) throws Exception {
        User user = verifyGoogleTokenAndGetUser(googleToken);

        if (user.isTwoFactorEnabled()) {
            return new TwoFaSetupResponse(false, "2FA jest już aktywne. Wyłącz je najpierw.", null);
        }

        String secret = secretGenerator.generate();
        user.setTwoFactorSecret(secret);
        userRepository.save(user);

        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer("Bazunia App")
                .build();

        // Zwracamy URL, Android sam wygeneruje QR code
        return new TwoFaSetupResponse(true, secret, data.getUri());
    }

    public boolean verifyAndEnableTwoFa(String googleToken, String totpCode) throws Exception {
        User user = verifyGoogleTokenAndGetUser(googleToken);

        if (user.getTwoFactorSecret() == null) {
            throw new IllegalStateException("Sekret 2FA nie został wygenerowany.");
        }

        if (codeVerifier.isValidCode(user.getTwoFactorSecret(), totpCode)) {
            user.setTwoFactorEnabled(true);
            user.setLastTwoFactorLogin(LocalDateTime.now());
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public boolean disableTwoFa(String googleToken, String totpCode) throws Exception {
        User user = verifyGoogleTokenAndGetUser(googleToken);

        if (!user.isTwoFactorEnabled() || user.getTwoFactorSecret() == null) {
            throw new IllegalStateException("2FA nie jest włączone.");
        }

        if (codeVerifier.isValidCode(user.getTwoFactorSecret(), totpCode)) {
            user.setTwoFactorEnabled(false);
            user.setTwoFactorSecret(null);
            user.setLastTwoFactorLogin(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public LoginResponse loginVerifyTwoFa(String googleToken, String totpCode) throws Exception {
        User user = verifyGoogleTokenAndGetUser(googleToken);

        if (!user.isTwoFactorEnabled() || user.getTwoFactorSecret() == null) {
            throw new IllegalStateException("2FA nie jest wymagane.");
        }

        if (codeVerifier.isValidCode(user.getTwoFactorSecret(), totpCode)) {
            user.setLastTwoFactorLogin(LocalDateTime.now());
            userRepository.save(user);

            String jwt = jwtService.generateToken(user);
            return new LoginResponse(jwt);
        } else {
            throw new SecurityException("Nieprawidłowy kod 2FA.");
        }
    }

    public record AuthResponse(String jwt, boolean requires2FA, String message) {}


}