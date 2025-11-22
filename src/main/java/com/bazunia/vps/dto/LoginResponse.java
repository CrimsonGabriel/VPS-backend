package com.bazunia.vps.dto;

public record LoginResponse(String token, boolean requiresPasswordSetup) {}