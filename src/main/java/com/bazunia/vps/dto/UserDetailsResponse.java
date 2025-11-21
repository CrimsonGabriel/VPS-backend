package com.bazunia.vps.dto;

public record UserDetailsResponse(
        Long id,
        String email,
        String name,
        String role
) {}