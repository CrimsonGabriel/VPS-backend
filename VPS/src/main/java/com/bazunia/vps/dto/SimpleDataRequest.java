package com.bazunia.vps.dto;

import java.util.List;

public record SimpleDataRequest(
        String password,
        List<SimpleSensorReadingDto> sensors
) {}