package com.bazunia.vps.dto;

import com.bazunia.vps.model.Gateway;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO reprezentujące bramkę (z czujnikami), wysyłane do Androida.
 * Odłączone od sesji Hibernate.
 */
public record GatewayDto(
        Long id,
        String name,
        String status,
        String folder,
        String description,
        String lastSeen,
        List<SensorDto> sensors
) {
    // Konstruktor mapujący Encję na DTO
    public static GatewayDto fromEntity(Gateway entity) {
        // Konwertuj Set<Sensor> na List<SensorDto>
        List<SensorDto> sensorDtos = entity.getSensors().stream()
                .map(SensorDto::fromEntity)
                .collect(Collectors.toList());

        return new GatewayDto(
                entity.getId(),
                entity.getName(),
                entity.getStatus(),
                entity.getFolder(),
                entity.getDescription(),
                (entity.getLastSeen() != null) ? entity.getLastSeen().toString() : null,
                sensorDtos
        );
    }
}