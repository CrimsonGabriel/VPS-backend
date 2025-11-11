package com.bazunia.vps.dto;

// Używamy rekordu dla zwięzłości
public record EmailRegistrationRequest(String email, String password) {
}