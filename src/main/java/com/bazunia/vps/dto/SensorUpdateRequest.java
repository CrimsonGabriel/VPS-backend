package com.bazunia.vps.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Size;

/**
 * DTO do aktualizacji sensora (PATCH).
 * Wszystkie pola są opcjonalne. Jeśli pole ma wartość 'null' w żądaniu,
 * nie zostanie ono zaktualizowane w bazie danych.
 */

public record SensorUpdateRequest(
        Long gatewayId,

        @Size(max = 100)
        String name,

        @Size(max = 50)
        String type,

        LocalDateTime createdAt,

        @Size(max = 255)
        String description,

        Integer intervalSeconds,
        Double alarmThresholdLow,
        Double alarmThresholdHigh,
        Integer batteryLevel,

        @Size(max = 50)
        String keyword,

        Boolean reportingEnabled
) {}