package com.bazunia.vps.dto;


public record ResetPasswordRequest(
        String token,
        String newPassword
) {}