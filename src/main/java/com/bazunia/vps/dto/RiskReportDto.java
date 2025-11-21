package com.bazunia.vps.dto;

import java.util.List;

public record RiskReportDto(
        boolean isSafe,          // true = zielona tarcza, false = lista błędów
        int riskCount,           // liczba zagrożeń
        List<RiskItemDto> risks  // lista szczegółów
) {}