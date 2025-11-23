package com.bazunia.vps.dto;

public record AdminUserRequest(
        String email,
        String password,
        String name,
        boolean isAdmin,
        boolean enabled
) {}