package com.bazunia.vps.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record StatusResponse(
        String lastReportText,
        String registeredRPiIp,
        Set<String> androidClients,
        Set<String> rpiClients,
        List<String> authorizedUsers,
        Map<String, TwoFaStatusDto> users2FA
) {}