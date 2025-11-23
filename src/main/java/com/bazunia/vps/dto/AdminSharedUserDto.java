package com.bazunia.vps.dto;

public record AdminSharedUserDto(
        String email,
        String permissionLevel
) {}