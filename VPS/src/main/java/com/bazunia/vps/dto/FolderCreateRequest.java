package com.bazunia.vps.dto;

// Używamy record dla zwięzłości
public record FolderCreateRequest(
        String name,
        String color
) {}