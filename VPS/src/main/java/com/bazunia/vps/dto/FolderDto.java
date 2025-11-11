package com.bazunia.vps.dto;

import com.bazunia.vps.model.Folder;
import com.bazunia.vps.model.Gateway;
import com.bazunia.vps.model.Sensor; // ⭐️ NOWY IMPORT

import java.util.Set;
import java.util.stream.Collectors;

public record FolderDto(
        Long id,
        String name,
        String color,
        Set<Long> gatewayIds,
        Set<Long> sensorIds // ⭐️ NOWE POLE (Krok 3)

) {
    // Metoda fabryczna do konwersji z Encji
    public static FolderDto fromEntity(Folder folder) {

        Set<Long> gwIds = folder.getGateways().stream()
                .map(Gateway::getId)
                .collect(Collectors.toSet());

        // ⭐️ NOWA LOGIKA MAPOWANIA (Krok 3)
        Set<Long> sIds = folder.getSensors().stream()
                .map(Sensor::getId)
                .collect(Collectors.toSet());

        return new FolderDto(folder.getId(), folder.getName(), folder.getColor(), gwIds, sIds);
    }
}