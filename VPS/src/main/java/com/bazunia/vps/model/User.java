package com.bazunia.vps.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data
@Entity
@Table(name = "app_users")
public class User implements UserDetails { // Implementujemy UserDetails dla Spring Security

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email; // Będzie służył jako "username"

    private String name; // Imię (z Google, opcjonalne)

    @Column(nullable = true)
    private String password; // Będzie przechowywać HASH!

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    // Pola 2FA (zostawiamy, jak były)
    private String twoFactorSecret;
    private boolean twoFactorEnabled = false;
    private LocalDateTime lastTwoFactorLogin;

    // ⭐️ NOWE POLA DLA AKTYWACJI E-MAIL ⭐️
    @Column(nullable = false)
    private boolean enabled = false; // Domyślnie konto jest NIEAKTYWNE

    @Column(unique = true)
    private String activationToken; // Token do aktywacji e-mail

    // --- Magia Spring Security ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        // ⭐️ ZMODYFIKOWANE: Spring Security będzie teraz sprawdzać to pole ⭐️
        return this.enabled;
    }
}