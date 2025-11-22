package com.bazunia.vps.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "app_users")
@ToString(exclude = {"ownedGateways", "gatewayPermissions"}) // Wyklucz kolekcje z toString()
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ... (wszystkie inne pola: email, name, password, role, 2fa, tokeny...)
    @Column(unique = true, nullable = false)
    private String email;
    private String name;
    @Column
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

    // --- Relacje ---
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Gateway> ownedGateways;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserGatewayPermission> gatewayPermissions;


    // --- Magia Spring Security ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }
    @Override
    public String getUsername() { return this.email; }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return this.enabled; }

    // --- POPRAWKA: Ręczne equals() i hashCode() ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode(); // Bezpieczne dla Hibernate
    }
}