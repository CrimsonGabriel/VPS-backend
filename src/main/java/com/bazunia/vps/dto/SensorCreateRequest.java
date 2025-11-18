package com.bazunia.vps.dto;
import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class SensorCreateRequest {

    // Pola obowiązkowe (zgodnie z Twoim nowym wymaganiem)
    @NotNull(message = "ID sensora jest wymagane")
    private Long id;

    @NotNull(message = "ID bramki jest wymagane")
    private Long gatewayId;

    @NotEmpty(message = "Nazwa sensora jest wymagana")
    @Size(max = 100)
    private String name;

    @NotEmpty(message = "Typ sensora jest wymagany")
    @Size(max = 50)
    private String type;

    @NotNull(message = "Data utworzenia jest wymagana")
    private LocalDateTime createdAt; // Np. "2025-11-17T22:30:00"

    // Pola nieobowiązkowe
    @Size(max = 255)
    private String description;

    private Integer intervalSeconds;
    private Double alarmThresholdLow;
    private Double alarmThresholdHigh;
    private Integer batteryLevel;

    @Size(max = 50)
    private String keyword;

    private Boolean reportingEnabled;
}