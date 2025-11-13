package com.bazunia.vps.dto;


public record GatewayUpdateRequest(
        String name,
        String description,
        String folder
) {}