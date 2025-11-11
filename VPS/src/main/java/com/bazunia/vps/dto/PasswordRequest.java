package com.bazunia.vps.dto;

// Używamy tego do odbierania hasła od RPi w endpointach POST
public record PasswordRequest(
        String password
) {
}