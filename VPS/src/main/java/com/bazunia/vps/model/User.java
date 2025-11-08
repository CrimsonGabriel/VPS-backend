package com.bazunia.vps.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set; // <<< ZMIANA: Dodano import

@Data
@Entity
@Table(name = "app_users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    @Column(nullable = true)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    private String twoFactorSecret;
    private boolean twoFactorEnabled = false;
    private LocalDateTime lastTwoFactorLogin;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(unique = true)
    private String activationToken;

    @Column(unique = true)
    private String passwordResetToken;

    private LocalDateTime passwordResetTokenExpiry;

    // --- Relacje dodane dla nowych tabel ---

    // <<< ZMIANA: Bramki, których ten użytkownik jest właścicielem
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Gateway> ownedGateways;

    // <<< ZMIANA: Uprawnienia (udostępnienia) dla tego użytkownika
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserGatewayPermission> gatewayPermissions;


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
        return this.enabled;
    }
}