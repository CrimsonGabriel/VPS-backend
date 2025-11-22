package com.bazunia.vps.dto;

import java.util.List;

public record RiskReportDto(
        boolean isSafe,
        int riskCount,
        List<RiskItemDto> risks
) {}