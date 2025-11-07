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

    // WYMÓG #2: Dodajemy pole na hasło
    // Jest nullable (może być puste), bo użytkownicy z Google NIE BĘDĄ mieli hasła!
    @Column(nullable = true)
    private String password; // Będzie przechowywać HASH!

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    // Pola 2FA (zostawiamy, jak były)
    private String twoFactorSecret;
    private boolean twoFactorEnabled = false;
    private LocalDateTime lastTwoFactorLogin;

    // --- Magia Spring Security ---
    // Te metody są potrzebne, żeby działało logowanie login/hasło

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Na razie każdy jest USEREM
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }

    @Override
    public String getUsername() {
        // Naszym "username" jest email
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}