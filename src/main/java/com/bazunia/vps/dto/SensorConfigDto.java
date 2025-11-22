package com.bazunia.vps.dto;

import com.bazunia.vps.model.Sensor;


public record SensorConfigDto(
        Long id,
        Long gatewayId,
        String type,
        Integer intervalSeconds,
        boolean reportingEnabled
) {

    /**
     * Metoda fabryczna do łatwej konwersji Encji Sensor na to DTO.
     */

    public static SensorConfigDto fromEntity(Sensor sensor) {
        return new SensorConfigDto(
                sensor.getId(),
                sensor.getGateway() != null ? sensor.getGateway().getId() : null,
                sensor.getType(),
                sensor.getIntervalSeconds(),
                sensor.isReportingEnabled()
        );
    }
}