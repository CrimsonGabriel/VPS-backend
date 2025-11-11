package com.bazunia.vps.dto;

import java.util.List;


public record BatteryUpdateRequest(
        String password,
        List<BatteryStatusRequest> statuses
) {}