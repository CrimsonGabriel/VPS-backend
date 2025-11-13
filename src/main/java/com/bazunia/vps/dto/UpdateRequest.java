package com.bazunia.vps.dto;

// Ten rekord jest potrzebny dla endpointu /update
public record UpdateRequest(String password, String message) {
}