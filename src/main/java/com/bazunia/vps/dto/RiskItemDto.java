package com.bazunia.vps.dto;

public record RiskItemDto(
        String sensorName,  // np. "Okno Kuchnia"
        String issue,       // np. "Otwarte" lub "Temp > 25C"
        String iconType     // np. "window", "fire", "light" - żeby Android wiedział jaką ikonę dać
) {}