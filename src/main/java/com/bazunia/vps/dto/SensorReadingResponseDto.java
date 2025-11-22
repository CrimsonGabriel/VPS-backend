package com.bazunia.vps.dto;

import java.io.Serializable;

public record SensorReadingResponseDto(
        String gatewayId,
        String sensorId,
        String type,
        String value,
        long timestamp,
        Long id
) implements Serializable {

    /**
     * Konstruktor idealnie dopasowany do Twojej encji SensorReading:
     * 1. r.gateway.id -> Long
     * 2. r.sensor.id -> Long
     * 3. r.sensor.type -> String (z Sensor.java)
     * 4. r.value -> String (z SensorReading.java)
     * 5. r.timestamp -> long (z SensorReading.java)
     * 6. r.id -> Long
     */
    public SensorReadingResponseDto(Long gatewayId, Long sensorId, String type, String value, long timestamp, Long id) {
        this(
                String.valueOf(gatewayId),
                String.valueOf(sensorId),
                type,
                value,
                timestamp,
                id
        );
    }
}