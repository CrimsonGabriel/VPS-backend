package com.bazunia.vps.dto;

public record RiskItemDto(
        String sensorName,
        String issue,
        String iconType
) {}