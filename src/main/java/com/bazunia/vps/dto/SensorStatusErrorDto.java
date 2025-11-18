package com.bazunia.vps.dto;

public record SensorStatusErrorDto(
        Long entityId, // ID bramki lub czujnika
        String entityType, // "GATEWAY" lub "SENSOR"
        String errorType, // "OFFLINE" lub "NO_DATA"
        String entityName,
        String readableMessage // "Bramka 'Kuchnia' jest offline od 30 minut."
) {}