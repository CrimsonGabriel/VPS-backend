package com.bazunia.vps.dto;

public record AdminSensorSummaryDto(
        String name,
        String type,
        Integer batteryLevel,
        boolean reportingEnabled
) {}