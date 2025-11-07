package com.bazunia.vps.service;
import java.util.logging.Logger;
import com.bazunia.vps.dto.LoginRequest;
import com.bazunia.vps.dto.LoginResponse;
import com.bazunia.vps.dto.RegisterRequest;
import com.bazunia.vps.dto.TwoFaSetupResponse;
import com.bazunia.vps.model.User;
import com.bazunia.vps.model.Role;
import com.bazunia.vps.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.bazunia.vps.dto.EmailRegistrationRequest;
import org.springframework.stereotype.Service;

// Importy dla 2FA
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.SecretGenerator;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

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

    // ⭐️ NOWA ZALEŻNOŚĆ DLA E-MAILI ⭐️
    private final EmailService emailService;

    // KLUCZ SERWERA DO WERYFIKACJI
    private static final String GOOGLE_CLIENT_ID = "79063316759-iva8uesd0vlj3in6eaeralk2kdkgv5or.apps.googleusercontent.com";
    private static final Logger logger = Logger.getLogger(AuthService.class.getName());

    @Autowired
    public AuthService(UserRepository userRepository, StatusService statusService, JwtService jwtService,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       SecretGenerator secretGenerator, CodeVerifier codeVerifier, EmailService emailService) { // Dodane 2FA
        this.userRepository = userRepository;
        this.statusService = statusService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.secretGenerator = secretGenerator;
        this.codeVerifier = codeVerifier;
        this.emailService = emailService;
    }

    private User verifyGoogleTokenAndGetUser(String googleToken) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                .build();

        GoogleIdToken idToken = verifier.verify(googleToken);
        if (idToken == null) {
            logger.warning("Niepowodzenie weryfikacji tokena Google. Token ID jest NULL.");
            throw new IllegalArgumentException("Nieprawidłowy token Google.");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        logger.info("Pomyślna weryfikacja Google dla email: " + email);

        // Znajdź lub stwórz usera
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setTwoFactorEnabled(false);
            newUser.setEnabled(true); // ⭐️ UŻYTKOWNICY GOOGLE SĄ AKTYWNI OD RAZU ⭐️
            logger.info("Tworzenie nowego usera (Google): " + email);
            return userRepository.save(newUser);
        });
    }

    // --- LOGIKA DLA ANDROIDA (Google Auth) ---

    public AuthResponse loginWithGoogle(String googleToken, String ipAddress) {
        try {
            User user = verifyGoogleTokenAndGetUser(googleToken);
            statusService.addAndroidIp(ipAddress);

            // ⭐️⭐️ ZMODYFIKOWANE: Użycie nowej metody pomocniczej ⭐️⭐️
            boolean requires2FA = is2FaRequired(user);

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
        // Upewnijmy się, że nowi użytkownicy z panelu admina też są aktywni
        user.setEnabled(true);
        userRepository.save(user);
    }

    public AuthResponse loginUser(LoginRequest request, String ipAddress) {
        // Krok 1: Uwierzytelnij e-mail i hasło
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userRepository.findByEmail(request.email()).orElseThrow();

        // Krok 2: Zarejestruj IP (tak jak wcześniej)
        statusService.addAndroidIp(ipAddress);

        // ⭐️⭐️ ZMODYFIKOWANE: Użycie nowej metody pomocniczej ⭐️⭐️
        boolean requires2FA = is2FaRequired(user);

        // Krok 4: Wygeneruj token JWT
        String jwt = jwtService.generateToken(user);

        if (requires2FA) {
            return new AuthResponse(jwt, true, "Wymagane 2FA");
        } else {
            return new AuthResponse(jwt, false, "Zalogowano przez Email");
        }
    }

    // === ⭐️ NOWA LOGIKA DLA ENDPOINTÓW 2FA (Przeniesiona z server.js) ⭐️ ===

    public boolean getTwoFaStatus(String jwtToken) throws Exception { User user = getUserFromJwt(jwtToken);
        return user.isTwoFactorEnabled();
    }

    public TwoFaSetupResponse setupTwoFa(String jwtToken) throws Exception { User user = getUserFromJwt(jwtToken);

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

    public boolean verifyAndEnableTwoFa(String jwtToken, String totpCode) throws Exception { User user = getUserFromJwt(jwtToken);

        if (user.getTwoFactorSecret() == null) {
            throw new IllegalStateException("Sekret 2FA nie został wygenerowany.");
        }
        // ⭐️⭐️ ZMODYFIKOWANE: Użycie nowej metody pomocniczej ⭐️⭐️
        // Logika weryfikacji została przeniesiona, ale musimy zapisać stan
        if (codeVerifier.isValidCode(user.getTwoFactorSecret(), totpCode)) {
            user.setTwoFactorEnabled(true);
            user.setLastTwoFactorLogin(LocalDateTime.now());
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public boolean disableTwoFa(String jwtToken, String totpCode) throws Exception { User user = getUserFromJwt(jwtToken);

        if (!user.isTwoFactorEnabled() || user.getTwoFactorSecret() == null) {
            throw new IllegalStateException("2FA nie jest włączone.");
        }
        // ⭐️⭐️ ZMODYFIKOWANE: Użycie nowej metody pomocniczej ⭐️⭐️
        // Logika weryfikacji została przeniesiona, ale musimy zapisać stan
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

        // ⭐️⭐️ ZMODYFIKOWANE: Użycie nowej metody pomocniczej ⭐️⭐️
        return verify2FaCodeAndGenerateJwt(user, totpCode);
    }

    public record AuthResponse(String jwt, boolean requires2FA, String message) {}

    // ⭐️⭐️ NOWA METODA ⭐️⭐️
    public void registerAndroidUser(EmailRegistrationRequest request) {
        // 1. Sprawdź, czy e-mail już istnieje
        if (userRepository.findByEmail(request.email()).isPresent()) {
            // Rzucamy wyjątek, który kontroler zamieni na 409 Conflict
            throw new IllegalStateException("Email już zajęty");
        }

        // 2. Stwórz nowego użytkownika
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setEnabled(false); // ⭐️ WAŻNE: Konto jest NIEAKTYWNE

        // 3. Wygeneruj token aktywacyjny
        String token = UUID.randomUUID().toString();
        user.setActivationToken(token);

        // 4. Zapisz użytkownika w bazie
        userRepository.save(user);

        // 5. Wyślij e-mail aktywacyjny (zakładając, że masz EmailService)
        // Musisz zaimplementować EmailService i skonfigurować SMTP w application.properties!
        String activationLink = "http://TWOJ_ADRES_IP:8080/api/auth/android/activate?token=" + token;
        emailService.sendActivationEmail(user.getEmail(), activationLink);
    }

    // ⭐️⭐️ NOWA METODA ⭐️⭐️
    public void activateUser(String token) {
        // 1. Znajdź użytkownika po tokenie
        // (Musisz dodać metodę findByActivationToken do swojego UserRepository!)
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy token aktywacyjny"));

        // 2. Aktywuj konto i usuń token
        user.setEnabled(true);
        user.setActivationToken(null); // Token jest jednorazowy
        userRepository.save(user);

        // Opcjonalnie: możesz wysłać e-mail "Witaj, Twoje konto jest aktywne"
        // emailService.sendWelcomeEmail(user.getEmail());
    }

    // ⭐️⭐️ NOWA METODA DO WERYFIKACJI 2FA PO ZALOGOWANIU E-MAILEM ⭐️⭐️
    public LoginResponse loginVerifyEmailTwoFa(String tempToken, String totpCode) throws Exception {
        // Krok 1: Użyj JwtService, aby wyodrębnić email z tokena tymczasowego
        String email = jwtService.extractUsername(tempToken);
        if (email == null) {
            throw new SecurityException("Nieprawidłowy token tymczasowy.");
        }

        // Krok 2: Pobierz użytkownika
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Użytkownik z tokena nie znaleziony."));

        // ⭐️⭐️ ZMODYFIKOWANE: Użycie nowej metody pomocniczej ⭐️⭐️
        return verify2FaCodeAndGenerateJwt(user, totpCode);
    }


    // ⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️
    // ⭐️⭐️               NOWE METODY POMOCNICZE                 ⭐️⭐️
    // ⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️⭐️

    /**
     * (Refaktoryzacja) Sprawdza, czy 2FA jest włączone i czy minął 5-minutowy okres karencji.
     * To jest "Duplikacja 9 linii".
     */
    private boolean is2FaRequired(User user) {
        if (user.isTwoFactorEnabled()) {
            if (user.getLastTwoFactorLogin() == null ||
                    user.getLastTwoFactorLogin().isBefore(LocalDateTime.now().minusMinutes(5))) {
                return true;
            }
        }
        return false;
    }

    /**
     * (Refaktoryzacja) Weryfikuje kod 2FA i zwraca ostateczny LoginResponse z tokenem JWT.
     * To jest "Duplikacja 11 linii".
     */
    private LoginResponse verify2FaCodeAndGenerateJwt(User user, String totpCode) {
        if (!user.isTwoFactorEnabled() || user.getTwoFactorSecret() == null) {
            throw new IllegalStateException("2FA nie jest wymagane dla tego użytkownika.");
        }

        if (codeVerifier.isValidCode(user.getTwoFactorSecret(), totpCode)) {
            user.setLastTwoFactorLogin(LocalDateTime.now());
            userRepository.save(user);

            // Wygeneruj NOWY, PEŁNY token JWT.
            String jwt = jwtService.generateToken(user);
            return new LoginResponse(jwt);
        } else {
            throw new SecurityException("Nieprawidłowy kod 2FA.");
        }
    }

    // ⭐️⭐️ NOWA METODA POMOCNICZA ⭐️⭐️
    private User getUserFromJwt(String jwtToken) {
        // Używamy JwtService do wyodrębnienia emaila z tokena
        String email = jwtService.extractUsername(jwtToken);
        if (email == null) {
            throw new SecurityException("Nieprawidłowy token JWT.");
        }
        // Znajdujemy użytkownika w bazie
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Użytkownik z tokena JWT nie znaleziony."));
    }
}