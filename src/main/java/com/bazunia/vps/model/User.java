package com.bazunia.vps.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "app_users") // "user" to słowo kluczowe w SQL, lepiej użyć innej nazwy
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email; // Odpowiednik klucza z userSecrets2FA

    private String name; // Imię z Google

    // Odpowiedniki pól z userSecrets2FA
    private String twoFactorSecret; // user.secret
    private boolean twoFactorEnabled = false; // user.enabled
    private LocalDateTime lastTwoFactorLogin; // user.last_2fa_verified_at

    // Nie przechowujemy JWT w bazie!
    // JWT jest "bezstanowy". Jest generowany przy logowaniu i wysyłany do klienta.
    // Serwer go nie zapisuje, tylko weryfikuje jego podpis.
}