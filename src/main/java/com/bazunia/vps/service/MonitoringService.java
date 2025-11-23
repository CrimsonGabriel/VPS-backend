package com.bazunia.vps.service;

import com.bazunia.vps.dto.RiskItemDto;
import com.bazunia.vps.dto.RiskReportDto;
import com.bazunia.vps.dto.SensorStatusErrorDto;
import com.bazunia.vps.model.Gateway;
import com.bazunia.vps.model.Sensor;
import com.bazunia.vps.model.SensorReading;
import com.bazunia.vps.model.User;
import com.bazunia.vps.repository.GatewayRepository;
import com.bazunia.vps.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final GatewayRepository gatewayRepository;
    private final SensorReadingRepository sensorReadingRepository;

    @Transactional(readOnly = true)
    public List<SensorStatusErrorDto> checkSensorStatuses(User user) {
        final LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        List<SensorStatusErrorDto> errors = new ArrayList<>();

        List<Gateway> gateways = gatewayRepository.findAllOwnedAndShared(user.getId());

        for (Gateway gateway : gateways) {
            LocalDateTime lastSeen = gateway.getLastSeen();
            if (lastSeen == null || lastSeen.isBefore(cutoff)) {
                String lastSeenText;
                if (lastSeen == null) {
                    lastSeenText = "nigdy";
                } else {
                    long minutesAgo = ChronoUnit.MINUTES.between(lastSeen, LocalDateTime.now());
                    lastSeenText = minutesAgo == 0 ? "ponad 5 min. temu" : "ponad " + minutesAgo + " min. temu";
                }

                String message = "Bramka '" + gateway.getName() + "' jest offline. Ostatni kontakt: " + lastSeenText + ".";
                errors.add(new SensorStatusErrorDto(
                        gateway.getId(), "GATEWAY", gateway.getName(), "OFFLINE", message
                ));
            }
        }
        return errors;
    }

    public RiskReportDto generateRiskReport(User user) {
        List<RiskItemDto> risks = new ArrayList<>();
        List<Gateway> userGateways = gatewayRepository.findAllOwnedAndShared(user.getId());

        for (Gateway gw : userGateways) {
            if (gw.getSensors() == null) continue;

            for (Sensor sensor : gw.getSensors()) {
                if (!sensor.isReportingEnabled()) continue;

                Optional<SensorReading> lastReadingOpt = sensorReadingRepository.findTopBySensorOrderByTimestampDesc(sensor);

                lastReadingOpt.ifPresent(sensorReading -> analyzeReading(sensor, sensorReading, risks));
            }
        }
        return new RiskReportDto(risks.isEmpty(), risks.size(), risks);
    }

    private void analyzeReading(Sensor sensor, SensorReading reading, List<RiskItemDto> risks) {
        String valStr = reading.getValue();
        String type = sensor.getType() != null ? sensor.getType().toUpperCase() : "";
        String name = sensor.getName();

        try {
            if (type.contains("LIGHT") || type.contains("SWIATLO")) {
                if (Double.parseDouble(valStr) > 0.5) {
                    risks.add(new RiskItemDto(name, "Światło włączone", "light"));
                }
            }
            if (type.contains("DOOR") || type.contains("WINDOW") || type.contains("KONTAKTRON")) {
                if ("0.0".equals(valStr) || "0".equals(valStr)) {
                    risks.add(new RiskItemDto(name, "Otwarte", "window"));
                }
            }
            double val = Double.parseDouble(valStr);
            if (sensor.getAlarmThresholdHigh() != null && val > sensor.getAlarmThresholdHigh()) {
                risks.add(new RiskItemDto(name, "Przekroczono MAX: " + val, "warning"));
            }
            if (sensor.getAlarmThresholdLow() != null && val < sensor.getAlarmThresholdLow()) {
                risks.add(new RiskItemDto(name, "Poniżej MIN: " + val, "warning"));
            }
        } catch (NumberFormatException ignored) { }
    }
}