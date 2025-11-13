package com.bazunia.vps.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record StatusResponse(
        String lastReportText,
        String registeredRPiIp,

        // Zmieniona nazwa, aby pasowała do server.js
        Set<String> registeredAndroidIps,

        // Dodane pole z server.js
        List<String> authorizedUsers,

        // Dodane pole z server.js
        Map<String, TwoFaStatusDto> users2FAStatus
) {
}