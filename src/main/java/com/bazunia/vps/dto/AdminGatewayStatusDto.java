package com.bazunia.vps.dto;

import java.util.List;

public record AdminGatewayStatusDto(
        Long id,
        String name,
        String ownerEmail,
        String status,
        String lastSeen,
        String description,
        String folder,
        List<AdminSensorSummaryDto> sensors,
        List<AdminSharedUserDto> sharedWith
) {}