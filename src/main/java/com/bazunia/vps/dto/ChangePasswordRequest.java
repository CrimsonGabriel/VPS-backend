package com.bazunia.vps.dto;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {}