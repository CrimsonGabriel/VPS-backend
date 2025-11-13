package com.bazunia.vps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// To DTO idealnie mapuje "płaski" JSON wysyłany przez skrypt RPi
public record SimpleSensorReadingDto(
        @JsonProperty("gateway_id") String gateway_id,
        @JsonProperty("sensor_id") String sensor_id,
        String type,
        String value,
        long timestamp
) {}