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