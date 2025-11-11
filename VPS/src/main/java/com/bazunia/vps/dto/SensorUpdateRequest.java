package com.bazunia.vps.dto;


public record SensorUpdateRequest(
        String name,
        String description,
        Integer intervalSeconds
) {}