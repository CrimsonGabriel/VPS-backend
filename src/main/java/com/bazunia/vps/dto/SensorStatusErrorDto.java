package com.bazunia.vps.dto;

public record SensorStatusErrorDto(
        Long entityId,
        String entityType,
        String errorType,
        String entityName,
        String readableMessage
) {}