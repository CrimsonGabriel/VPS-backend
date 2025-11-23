package com.bazunia.vps.dto;

import com.bazunia.vps.model.User;

public record AdminUserDto(
        Long id,
        String email,
        String name,
        String role,
        boolean enabled,
        boolean twoFactorEnabled
) {
    public static AdminUserDto fromEntity(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.isEnabled(),
                user.isTwoFactorEnabled()
        );
    }
}