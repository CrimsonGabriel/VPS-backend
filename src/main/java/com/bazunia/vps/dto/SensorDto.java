package com.bazunia.vps.dto;

import com.bazunia.vps.model.Sensor;

/**
 * DTO reprezentujące czujnik, wysyłane do Androida.
 * Odłączone od sesji Hibernate.
 */
public record SensorDto(
        Long id,
        String name,
        String type,
        String description,
        Integer batteryLevel,
        String keyword,
        boolean reportingEnabled
) {
    // Konstruktor mapujący Encję na DTO
    public static SensorDto fromEntity(Sensor entity) {
        return new SensorDto(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getDescription(),
                entity.getBatteryLevel(),
                entity.getKeyword(),
                entity.isReportingEnabled()
        );
    }
}