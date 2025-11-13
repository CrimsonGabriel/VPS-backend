// 💾 Role.java
package com.bazunia.vps.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public enum Role {
    USER,
    ADMIN;

    public Set<SimpleGrantedAuthority> getAuthorities() {
        // Zwraca uprawnienie z wymaganym prefiksem "ROLE_"
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + this.name()));
    }
}