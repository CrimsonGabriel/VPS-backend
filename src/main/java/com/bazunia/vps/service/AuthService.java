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
import com.bazunia.vps.dto.UserDetailsResponse;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.SecretGenerator;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Collections;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StatusService statusService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecretGenerator secretGenerator;
    private final CodeVerifier codeVerifier;
    private final EmailService emailService;

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


        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setTwoFactorEnabled(false);
            newUser.setEnabled(true);
            logger.info("Tworzenie nowego usera (Google): " + email);
            return userRepository.save(newUser);
        });
    }

    // --- LOGIKA DLA ANDROIDA (Google Auth) ---

    public AuthResponse loginWithGoogle(String googleToken, String ipAddress) {
        try {
            User user = verifyGoogleTokenAndGetUser(googleToken);
            statusService.addAndroidIp(ipAddress);

            boolean requires2FA = is2FaRequired(user);


            boolean requiresPasswordSetup = (user.getPassword() == null);

            if (requires2FA) {

                return new AuthResponse(null, true, "Wymagane 2FA", false);
            } else {
                String jwt = jwtService.generateToken(user);
                return new AuthResponse(jwt, false, "Zalogowano przez Google", requiresPasswordSetup);
            }
        } catch (Exception e) {
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
        user.setEnabled(true);
        userRepository.save(user);
    }

    public AuthResponse loginUser(LoginRequest request, String ipAddress) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userRepository.findByEmail(request.email()).orElseThrow();
        statusService.addAndroidIp(ipAddress);

        boolean requires2FA = is2FaRequired(user);
        String jwt = jwtService.generateToken(user);

        boolean requiresPasswordSetup = false;

        if (requires2FA) {
            return new AuthResponse(jwt, true, "Wymagane 2FA", requiresPasswordSetup);
        } else {
            return new AuthResponse(jwt, false, "Zalogowano przez Email", requiresPasswordSetup);
        }
    }

    // === NOWA LOGIKA DLA ENDPOINTÓW 2FA (Przeniesiona z server.js) ️===


    public boolean getTwoFaStatus(String jwtToken) {
        User user = getUserFromJwt(jwtToken);
        return user.isTwoFactorEnabled();
    }

    public TwoFaSetupResponse setupTwoFa(String jwtToken) {
        User user = getUserFromJwt(jwtToken);

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

        return new TwoFaSetupResponse(true, secret, data.getUri());
    }

    public boolean verifyAndEnableTwoFa(String jwtToken, String totpCode) {
        User user = getUserFromJwt(jwtToken);

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

    public boolean disableTwoFa(String jwtToken, String totpCode) {
        User user = getUserFromJwt(jwtToken);

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

    public LoginResponse loginVerifyTwoFa(String googleToken, String totpCode) {
        try {
            User user = verifyGoogleTokenAndGetUser(googleToken);

            return verify2FaCodeAndGenerateJwt(user, totpCode);

        } catch (Exception e) {
            throw new RuntimeException("Błąd weryfikacji: " + e.getMessage(), e);
        }
    }

    public record AuthResponse(String jwt, boolean requires2FA, String message, boolean requiresPasswordSetup) {}

    public void registerAndroidUser(EmailRegistrationRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("Email już zajęty");
        }

        // 2. Stwórz nowego użytkownika
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setEnabled(false);

        // 3. Wygeneruj token aktywacyjny
        String token = UUID.randomUUID().toString();
        user.setActivationToken(token);

        // 4. Zapisz użytkownika w bazie
        userRepository.save(user);

        // 5. Wyślij e-mail aktywacyjny (zakładając, że masz EmailService)
        String activationLink = "https://testserwera.pl/api/auth/android/activate?token=" + token;
        emailService.sendActivationEmail(user.getEmail(), activationLink);
    }

    public void activateUser(String token) {
        // 1. Znajdź użytkownika po tokenie
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy token aktywacyjny"));

        // 2. Aktywuj konto i usuń token
        user.setEnabled(true);
        user.setActivationToken(null); // Token jest jednorazowy
        userRepository.save(user);

        // Opcjonalnie: możesz wysłać e-mail "Witaj, Twoje konto jest aktywne"
        // emailService.sendWelcomeEmail(user.getEmail());
    }

    // METODA DO WERYFIKACJI 2FA PO ZALOGOWANIU E-MAILEM

    public LoginResponse loginVerifyEmailTwoFa(String tempToken, String totpCode) {
        String email = jwtService.extractUsername(tempToken);
        if (email == null) {
            throw new SecurityException("Nieprawidłowy token tymczasowy.");
        }

        // Krok 2: Pobierz użytkownika
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Użytkownik z tokena nie znaleziony."));

        return verify2FaCodeAndGenerateJwt(user, totpCode);
    }


    /**
     * (Refaktoryzacja) Sprawdza, czy 2FA jest włączone i czy minął 5-minutowy okres karencji.
     * To jest "Duplikacja 9 linii".
     */
    private boolean is2FaRequired(User user) {
        if (user.isTwoFactorEnabled()) {
            return user.getLastTwoFactorLogin() == null ||
                    user.getLastTwoFactorLogin().isBefore(LocalDateTime.now().minusMinutes(5));
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

            boolean requiresPasswordSetup = (user.getPassword() == null);

            String jwt = jwtService.generateToken(user);
            return new LoginResponse(jwt, requiresPasswordSetup);
        } else {
            throw new SecurityException("Nieprawidłowy kod 2FA.");
        }
    }

    private User getUserFromJwt(String jwtToken) {
        String email = jwtService.extractUsername(jwtToken);
        if (email == null) {
            throw new SecurityException("Nieprawidłowy token JWT.");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Użytkownik z tokena JWT nie znaleziony."));
    }
    /**
     * Ustawia hasło dla uwierzytelnionego użytkownika (np. po logowaniu Google).
     * Używa emaila wyciągniętego z tokena JWT, aby mieć pewność, że użytkownik
     * jest tym, za kogo się podaje.
     */
    public void setUserPassword(String jwtToken, String newPassword) {
        User user = getUserFromJwt(jwtToken);

        if (user.getPassword() != null) {
            throw new IllegalStateException("Użytkownik ma już ustawione hasło.");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Hasło jest zbyt krótkie.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Hasło zostało pomyślnie ustawione dla użytkownika: " + user.getEmail());
    }

    /**
     * Zmienia hasło uwierzytelnionego użytkownika.
     * Wymaga podania obecnego hasła w celu weryfikacji.
     *
     * @param jwtToken        Token JWT zalogowanego użytkownika
     * @param currentPassword Obecne (stare) hasło do weryfikacji
     * @param newPassword     Nowe hasło do ustawienia
     * @throws SecurityException        jeśli obecne hasło jest nieprawidłowe
     * @throws IllegalArgumentException jeśli nowe hasło jest za krótkie
     * @throws IllegalStateException    jeśli użytkownik (np. Google) nie ma ustawionego hasła
     */

    public void changeUserPassword(String jwtToken, String currentPassword, String newPassword) {
        // Krok 1: Znajdź użytkownika na podstawie tokena
        User user = getUserFromJwt(jwtToken);

        // Krok 2: Sprawdź, czy użytkownik w ogóle ma ustawione hasło
        if (user.getPassword() == null) {
            throw new IllegalStateException("Użytkownik nie ma ustawionego hasła, którego można by użyć do weryfikacji.");
        }

        // Krok 3: Zweryfikuj obecne hasło
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new SecurityException("Nieprawidłowe obecne hasło.");
        }

        // Krok 4: Zweryfikuj nowe hasło (np. długość)
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Nowe hasło jest za krótkie (minimum 8 znaków).");
        }

        // Krok 5: Jeśli wszystko OK, zakoduj i zapisz nowe hasło
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Hasło zostało pomyślnie zmienione dla użytkownika: " + user.getEmail());
    }
    /**
     * Pobiera podstawowe dane zalogowanego użytkownika (email i name).
     *
     * @param jwtToken Token JWT zalogowanego użytkownika
     * @return Obiekt DTO z danymi użytkownika
     */
    public UserDetailsResponse getUserDetails(String jwtToken) {
        User user = getUserFromJwt(jwtToken);

        return new UserDetailsResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name()
        );
    }

    /**
     * Rozpoczyna proces resetowania hasła dla użytkownika.
     * Znajduje użytkownika, generuje token i wysyła e-mail.
     * Celowo nie rzuca błędu, jeśli e-mail nie istnieje, aby zapobiec
     * wyliczeniu użytkowników (user enumeration).
     *
     * @param email E-mail podany przez użytkownika.
     */
    public void requestPasswordReset(String email) {
        // Krok 1: Znajdź użytkownika
        Optional<User> userOptional = userRepository.findByEmail(email);

        // Krok 2: Jeśli użytkownik nie istnieje LUB jest nieaktywny,
        if (userOptional.isEmpty() || !userOptional.get().isEnabled()) {
            logger.warning("Prośba o reset hasła dla nieistniejącego lub nieaktywnego e-maila: " + email);
            return; // Ciche wyjście
        }

        User user = userOptional.get();

        // Krok 3: Wygeneruj token i datę wygaśnięcia
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));

        // Krok 4: Zapisz token w bazie
        userRepository.save(user);

        // Krok 5: Wyślij e-mail

        String resetLink = "https://testserwera.pl/reset-password?token=" + token;

        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        logger.info("Pomyślnie wygenerowano i wysłano link resetujący dla: " + email);
    }

    /**
     * Weryfikuje token resetujący i ustawia nowe hasło dla użytkownika.
     *
     * @param token       Token z linku e-mail.
     * @param newPassword Nowe hasło podane przez użytkownika.
     * @throws IllegalArgumentException jeśli token jest nieprawidłowy, wygasł lub hasło jest za krótkie.
     */
    public void resetPassword(String token, String newPassword) {
        // Krok 1: Znajdź użytkownika po tokenie
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy lub już użyty token."));

        // Krok 2: Sprawdź, czy token nie wygasł
        if (user.getPasswordResetTokenExpiry() == null || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpiry(null);
            userRepository.save(user);
            throw new IllegalArgumentException("Token resetujący wygasł. Poproś o nowy link.");
        }

        // Krok 3: Sprawdź siłę nowego hasła
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Hasło musi mieć co najmniej 8 znaków.");
        }

        // Krok 4: Ustaw nowe hasło i unieważnij token
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null); // Użyty token staje się nieważny
        user.setPasswordResetTokenExpiry(null);

        userRepository.save(user);

        logger.info("Hasło zostało pomyślnie zresetowane dla: " + user.getEmail());
    }
}
