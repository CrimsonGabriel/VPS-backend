package com.bazunia.vps.dto;


public record BatteryStatusRequest(
        Long sensorId,
        Integer level
) {}