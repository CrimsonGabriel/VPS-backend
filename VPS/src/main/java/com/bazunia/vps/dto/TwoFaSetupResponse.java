package com.bazunia.vps.dto;

public record TwoFaSetupResponse(boolean success, String secret, String otpauth_url) {
}