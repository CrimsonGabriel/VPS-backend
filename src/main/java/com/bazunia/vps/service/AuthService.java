package com.bazunia.vps.service;

import com.bazunia.vps.dto.*;
import com.bazunia.vps.model.Role;
import com.bazunia.vps.model.User;
import com.bazunia.vps.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor // Lombok załatwia konstruktor
public class AuthService {

    private final UserRepository userRepository;
    private final StatusService statusService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final TwoFactorService twoFactorService; // Wstrzykujemy nowy serwis

    // Pobieramy z application.properties!
    @Value("${app.google.client-id}")
    private String googleClientId;

    @Value("${app.frontend.url:https://bazunia-vps.pl}")
    private String frontendUrl;

    private static final Logger logger = Logger.getLogger(AuthService.class.getName());

    public record AuthResponse(String jwt, boolean requires2FA, String message, boolean requiresPasswordSetup) {}

    // --- GOOGLE AUTH ---

    private User verifyGoogleTokenAndGetUser(String googleToken) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(googleToken);
        if (idToken == null) {
            logger.warning("Token Google ID jest NULL.");
            throw new IllegalArgumentException("Nieprawidłowy token Google.");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setRole(Role.USER);
            newUser.setTwoFactorEnabled(false);
            newUser.setEnabled(true);
            logger.info("Utworzono nowego użytkownika (Google): " + email);
            return userRepository.save(newUser);
        });
    }

    public AuthResponse loginWithGoogle(String googleToken, String ipAddress) {
        try {
            User user = verifyGoogleTokenAndGetUser(googleToken);
            statusService.addAndroidIp(ipAddress);

            boolean requires2FA = twoFactorService.is2FaRequired(user);
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

    // --- STANDARD LOGIN ---

    public AuthResponse loginUser(LoginRequest request, String ipAddress) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Błąd danych logowania"));

        statusService.addAndroidIp(ipAddress);

        boolean requires2FA = twoFactorService.is2FaRequired(user);
        String jwt = jwtService.generateToken(user);

        if (requires2FA) {
            return new AuthResponse(jwt, true, "Wymagane 2FA", false);
        } else {
            return new AuthResponse(jwt, false, "Zalogowano przez Email", false);
        }
    }

    // --- 2FA DELEGATION (Korzystamy z TwoFactorService) ---

    public boolean getTwoFaStatus(String jwtToken) {
        return getUserFromJwt(jwtToken).isTwoFactorEnabled();
    }

    public TwoFaSetupResponse setupTwoFa(String jwtToken) {
        return twoFactorService.setupTwoFa(getUserFromJwt(jwtToken));
    }

    public boolean verifyAndEnableTwoFa(String jwtToken, String totpCode) {
        return twoFactorService.verifyAndEnableTwoFa(getUserFromJwt(jwtToken), totpCode);
    }

    public boolean disableTwoFa(String jwtToken, String totpCode) {
        return twoFactorService.disableTwoFa(getUserFromJwt(jwtToken), totpCode);
    }

    public LoginResponse loginVerifyTwoFa(String googleToken, String totpCode) {
        try {
            User user = verifyGoogleTokenAndGetUser(googleToken);
            return finalize2FaLogin(user, totpCode);
        } catch (Exception e) {
            throw new RuntimeException("Błąd weryfikacji: " + e.getMessage(), e);
        }
    }

    public LoginResponse loginVerifyEmailTwoFa(String tempToken, String totpCode) {
        String email = jwtService.extractUsername(tempToken);
        if (email == null) throw new SecurityException("Nieprawidłowy token tymczasowy.");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Nie znaleziono użytkownika."));

        return finalize2FaLogin(user, totpCode);
    }

    private LoginResponse finalize2FaLogin(User user, String totpCode) {
        if (twoFactorService.validateCode(user, totpCode)) {
            twoFactorService.updateLastLogin(user); // Zapisujemy czas logowania w serwisie 2FA

            boolean requiresPasswordSetup = (user.getPassword() == null);
            String jwt = jwtService.generateToken(user);
            return new LoginResponse(jwt, requiresPasswordSetup);
        } else {
            throw new SecurityException("Nieprawidłowy kod 2FA.");
        }
    }

    // --- USER MANAGEMENT ---

    public void registerAndroidUser(EmailRegistrationRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("Email już zajęty");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setEnabled(false);

        String token = UUID.randomUUID().toString();
        user.setActivationToken(token);
        userRepository.save(user);

        // Używamy zmiennej frontendUrl zamiast hardcodu
        String activationLink = frontendUrl + "/activate?token=" + token;
        emailService.sendActivationEmail(user.getEmail(), activationLink);
    }

    public void activateUser(String token) {
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy token aktywacyjny"));
        user.setEnabled(true);
        user.setActivationToken(null);
        userRepository.save(user);
    }

    public void setUserPassword(String jwtToken, String newPassword) {
        User user = getUserFromJwt(jwtToken);
        if (user.getPassword() != null) throw new IllegalStateException("Użytkownik ma już hasło.");
        if (newPassword == null || newPassword.length() < 8) throw new IllegalArgumentException("Hasło za krótkie.");

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void changeUserPassword(String jwtToken, String currentPassword, String newPassword) {
        User user = getUserFromJwt(jwtToken);
        if (user.getPassword() == null) throw new IllegalStateException("Użytkownik nie ma hasła.");
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) throw new SecurityException("Błędne obecne hasło.");
        if (newPassword == null || newPassword.length() < 8) throw new IllegalArgumentException("Nowe hasło za krótkie.");

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public UserDetailsResponse getUserDetails(String jwtToken) {
        User user = getUserFromJwt(jwtToken);
        return new UserDetailsResponse(user.getId(), user.getEmail(), user.getName(), user.getRole().name());
    }

    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) return;

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token nieprawidłowy."));

        if (user.getPasswordResetTokenExpiry() == null || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token wygasł.");
        }

        if (newPassword == null || newPassword.length() < 8) throw new IllegalArgumentException("Hasło za krótkie.");

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
    }

    private User getUserFromJwt(String jwtToken) {
        String email = jwtService.extractUsername(jwtToken);
        if (email == null) throw new SecurityException("Nieprawidłowy token JWT.");
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Użytkownik z tokena nie znaleziony."));
    }
}