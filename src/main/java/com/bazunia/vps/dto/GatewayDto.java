package com.bazunia.vps.dto;

import com.bazunia.vps.model.Gateway;
import java.util.List;
import java.util.Collections;
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
        Long ownerId,
        List<SensorDto> sensors
) {
    // Konstruktor mapujący Encję na DTO
    public static GatewayDto fromEntity(Gateway entity) {
        // Konwertuj Set<Sensor> na List<SensorDto> (z zabezpieczeniem przed null)
        List<SensorDto> sensorDtos = (entity.getSensors() != null)
                ? entity.getSensors().stream()
                .map(SensorDto::fromEntity)
                .collect(Collectors.toList())
                : Collections.emptyList();

        // POPRAWKA: entity.getFolder() to już String, więc bierzemy go bezpośrednio
        String folderName = entity.getFolder();

        // Pobierz ID właściciela bezpiecznie
        Long ownerId = (entity.getOwner() != null) ? entity.getOwner().getId() : null;

        return new GatewayDto(
                entity.getId(),
                entity.getName(),
                entity.getStatus(),
                folderName,
                entity.getDescription(),
                (entity.getLastSeen() != null) ? entity.getLastSeen().toString() : null,
                ownerId,
                sensorDtos
        );
    }
}