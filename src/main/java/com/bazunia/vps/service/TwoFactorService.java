package com.bazunia.vps.service;

import com.bazunia.vps.dto.TwoFaSetupResponse;
import com.bazunia.vps.model.User;
import com.bazunia.vps.repository.UserRepository;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private final UserRepository userRepository;
    private final SecretGenerator secretGenerator;
    private final CodeVerifier codeVerifier;

    @Transactional
    public TwoFaSetupResponse setupTwoFa(User user) {
        if (user.isTwoFactorEnabled()) {
            return new TwoFaSetupResponse(false, "2FA jest już aktywne. Wyłącz je najpierw.", null);
        }

        String secret = secretGenerator.generate();
        user.setTwoFactorSecret(secret);
        userRepository.save(user);

        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer("Bazunia VPS")
                .build();

        return new TwoFaSetupResponse(true, secret, data.getUri());
    }

    @Transactional
    public boolean verifyAndEnableTwoFa(User user, String totpCode) {
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

    @Transactional
    public boolean disableTwoFa(User user, String totpCode) {
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

    public boolean validateCode(User user, String totpCode) {
        if (!user.isTwoFactorEnabled() || user.getTwoFactorSecret() == null) {
            return false;
        }
        return codeVerifier.isValidCode(user.getTwoFactorSecret(), totpCode);
    }

    @Transactional
    public void updateLastLogin(User user) {
        user.setLastTwoFactorLogin(LocalDateTime.now());
        userRepository.save(user);
    }

    public boolean is2FaRequired(User user) {
        if (user.isTwoFactorEnabled()) {
            // Wymagaj 2FA jeśli nigdy się nie logował 2FA lub minęło 5 minut
            return user.getLastTwoFactorLogin() == null ||
                    user.getLastTwoFactorLogin().isBefore(LocalDateTime.now().minusMinutes(5));
        }
        return false;
    }
}