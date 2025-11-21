package com.bazunia.vps.service;

import com.bazunia.vps.dto.GatewayDto;
import com.bazunia.vps.dto.GatewayUpdateRequest;
import com.bazunia.vps.model.Gateway;
import com.bazunia.vps.model.User;
import com.bazunia.vps.repository.GatewayRepository;
import com.bazunia.vps.repository.SensorReadingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bazunia.vps.dto.SensorUpdateRequest;
import com.bazunia.vps.dto.SensorConfigDto;
import com.bazunia.vps.model.Sensor;
import com.bazunia.vps.repository.SensorRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.bazunia.vps.dto.SensorStatusErrorDto;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import com.bazunia.vps.model.SystemSetting;
import com.bazunia.vps.repository.SystemSettingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bazunia.vps.dto.SensorReadingResponseDto;
import java.time.ZoneId;
import com.bazunia.vps.dto.RiskItemDto;
import com.bazunia.vps.dto.RiskReportDto;
import java.util.Optional;
import com.bazunia.vps.model.SensorReading;
import com.bazunia.vps.repository.UserGatewayPermissionRepository;
import com.bazunia.vps.model.UserGatewayPermission;
import com.bazunia.vps.model.PermissionLevel;

@Service
public class DataService {

    private static final Logger log = LoggerFactory.getLogger(DataService.class);

    private final SensorReadingRepository sensorReadingRepository;
    private final GatewayRepository gatewayRepository;
    private final SensorRepository sensorRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final UserGatewayPermissionRepository permissionRepository;

    @Autowired
    public DataService(SensorReadingRepository sensorReadingRepository,
                       GatewayRepository gatewayRepository,
                       SensorRepository sensorRepository,
                       SystemSettingRepository systemSettingRepository,
                       UserGatewayPermissionRepository permissionRepository) { // Wstrzykujemy nowe repo
        this.sensorReadingRepository = sensorReadingRepository;
        this.gatewayRepository = gatewayRepository;
        this.sensorRepository = sensorRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public void deleteAllSensorReadings() {
        sensorReadingRepository.deleteAll();
    }

    @Transactional(readOnly = true)
    public List<GatewayDto> getGatewaysForUser(User user) {
        List<Gateway> gateways = gatewayRepository.findAllOwnedAndShared(user.getId());
        return gateways.stream()
                .map(GatewayDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Aktualizacja bramki. Wymaga bycia WŁAŚCICIELEM lub posiadania uprawnień FULL_ACCESS.
     */
    @Transactional
    public Gateway updateGateway(Long gatewayId, GatewayUpdateRequest request, User user) {
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Bramka o ID " + gatewayId + " nie znaleziona."));

        // Sprawdź uprawnienia (Owner lub Full Access)
        validateWriteAccess(gateway, user);

        gateway.setName(request.name());
        gateway.setDescription(request.description());
        gateway.setFolder(request.folder());

        return gatewayRepository.save(gateway);
    }

    /**
     * Aktualizacja czujnika. Wymaga bycia WŁAŚCICIELEM bramki lub uprawnień FULL_ACCESS.
     */
    @Transactional
    public Sensor updateSensor(Long sensorId, SensorUpdateRequest request, User user) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new EntityNotFoundException("Czujnik o ID " + sensorId + " nie znaleziony."));

        // Sprawdź uprawnienia do bramki nadrzędnej
        validateWriteAccess(sensor.getGateway(), user);

        if (request.name() != null && !request.name().isEmpty()) {
            sensor.setName(request.name());
        }
        if (request.description() != null) {
            sensor.setDescription(request.description());
        }
        if (request.intervalSeconds() != null) {
            sensor.setIntervalSeconds(request.intervalSeconds());
        }

        return sensorRepository.save(sensor);
    }
    /**
     * Przełączanie raportowania. Wymaga Ownera lub FULL_ACCESS.
     */
    @Transactional
    public Sensor toggleSensorReporting(Long sensorId, User user) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new EntityNotFoundException("Czujnik o ID " + sensorId + " nie znaleziony."));

        validateWriteAccess(sensor.getGateway(), user);

        sensor.setReportingEnabled(!sensor.isReportingEnabled());
        return sensorRepository.save(sensor);
    }

    /**
     * Usuwanie przypisania bramki - TYLKO DLA WŁAŚCICIELA.
     * Współużytkownik nie może "usunąć" bramki (może tylko odejść, co robi się innym endpointem).
     */
    @Transactional
    public void disassociateGateway(Long gatewayId, User user) {
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Bramka o ID " + gatewayId + " nie znaleziona."));

        // Tutaj twarda weryfikacja - tylko właściciel może usunąć/odpiąć bramkę całkowicie
        if (!Objects.equals(gateway.getOwner().getId(), user.getId())) {
            throw new AccessDeniedException("Tylko właściciel może usunąć bramkę.");
        }

        // Usuwamy też wszystkie udostępnienia, żeby nie wisiały śmieci
        permissionRepository.deleteAllByGatewayId(gatewayId);

        gateway.setOwner(null);
        gatewayRepository.save(gateway);
    }

    // ========================================================
    // === METODY POMOCNICZE (Uprawnienia) ===
    // ========================================================

    /**
     * Sprawdza, czy użytkownik ma prawo do modyfikacji bramki (Owner lub FULL_ACCESS).
     */
    private void validateWriteAccess(Gateway gateway, User user) {
        // 1. Czy jest właścicielem?
        if (Objects.equals(gateway.getOwner().getId(), user.getId())) {
            return; // OK, jest właścicielem
        }

        // 2. Jeśli nie, czy ma uprawnienie FULL_ACCESS?
        Optional<UserGatewayPermission> permission = permissionRepository.findByGatewayIdAndUserId(gateway.getId(), user.getId());

        if (permission.isPresent() && permission.get().getPermissionLevel() == PermissionLevel.FULL_ACCESS) {
            return; // OK, ma pełny dostęp
        }

        // Brak dostępu
        throw new AccessDeniedException("Brak uprawnień do edycji (wymagany status Właściciela lub poziom PEŁNY DOSTĘP).");
    }

    // ========================================================
    // === STATUSY I RAPORTY (Dostępne też dla VIEW) ===
    // ========================================================

    @Transactional(readOnly = true)
    public List<SensorConfigDto> getAllSensorConfigs() {
        return sensorRepository.findAll().stream()
                .map(SensorConfigDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public int setGlobalSensorInterval(Integer interval) {
        if (interval == null || interval <= 0) {
            interval = 60;
        }
        return sensorRepository.setGlobalInterval(interval);
    }

    // UWAGA: Usunięto zduplikowaną metodę toggleSensorReporting (jest już wyżej w kodzie)

    @Transactional(readOnly = true)
    public List<SensorStatusErrorDto> checkSensorStatuses(User user) {
        final LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        List<SensorStatusErrorDto> errors = new ArrayList<>();

        // POPRAWKA: Pobieramy bramki własne ORAZ udostępnione
        List<Gateway> gateways = gatewayRepository.findAllOwnedAndShared(user.getId());

        for (Gateway gateway : gateways) {
            LocalDateTime lastSeen = gateway.getLastSeen();
            if (lastSeen == null || lastSeen.isBefore(cutoff)) {
                String lastSeenText;
                if (lastSeen == null) {
                    lastSeenText = "nigdy";
                } else {
                    long minutesAgo = ChronoUnit.MINUTES.between(lastSeen, LocalDateTime.now());
                    if (minutesAgo == 0) {
                        lastSeenText = "ponad 5 min. temu";
                    } else {
                        lastSeenText = "ponad " + minutesAgo + " min. temu";
                    }
                }

                String message = "Bramka '" + gateway.getName() + "' jest offline. Ostatni kontakt: " + lastSeenText + ".";
                errors.add(new SensorStatusErrorDto(
                        gateway.getId(),
                        "GATEWAY",
                        gateway.getName(),
                        "OFFLINE",
                        message
                ));
            }
        }
        return errors;
    }

    // ========================================================
    // === AUTOMATYCZNE CZYSZCZENIE (Retention Policy) ===
    // ========================================================

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void performAutoCleanup() {
        log.info("Rozpoczynanie automatycznego czyszczenia historii...");

        // 1. Czyszczenie po czasie (Dni)
        String daysStr = getSettingValue("RETENTION_DAYS");
        if (daysStr != null && !daysStr.isEmpty()) {
            try {
                int days = Integer.parseInt(daysStr);
                if (days > 0) {
                    long cutoffMillis = LocalDateTime.now()
                            .minusDays(days)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli();

                    sensorReadingRepository.deleteByTimestampLessThan(cutoffMillis);
                    log.info("Usunięto rekordy starsze niż {} dni (timestamp < {}).", days, cutoffMillis);
                }
            } catch (NumberFormatException e) {
                log.error("Błąd formatu RETENTION_DAYS: {}", daysStr);
            }
        }

        // 2. Czyszczenie po ilości (Max Rekordów)
        String maxRecStr = getSettingValue("RETENTION_MAX_RECORDS");
        if (maxRecStr != null && !maxRecStr.isEmpty()) {
            try {
                long maxRecords = Long.parseLong(maxRecStr);
                if (maxRecords > 0) {
                    trimToSize(maxRecords);
                }
            } catch (NumberFormatException e) {
                log.error("Błąd formatu RETENTION_MAX_RECORDS: {}", maxRecStr);
            }
        }
        log.info("Automatyczne czyszczenie zakończone.");
    }

    private void trimToSize(long limit) {
        long count = sensorReadingRepository.count();
        if (count <= limit) return;

        var pageRequest = PageRequest.of((int) limit, 1, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<SensorReadingResponseDto> result = sensorReadingRepository.findLatestReadingsWithDetails(pageRequest);

        if (!result.isEmpty()) {
            long cutoffTimestamp = result.get(0).timestamp();
            sensorReadingRepository.deleteByTimestampLessThan(cutoffTimestamp);
            log.info("Przycięto tabelę do {} rekordów (limit).", limit);
        }
    }

    public void saveRetentionSettings(Integer days, Long maxRecords) {
        saveSetting("RETENTION_DAYS", days != null ? days.toString() : "");
        saveSetting("RETENTION_MAX_RECORDS", maxRecords != null ? maxRecords.toString() : "");
    }

    public RetentionConfigDto getRetentionSettings() {
        String days = getSettingValue("RETENTION_DAYS");
        String max = getSettingValue("RETENTION_MAX_RECORDS");

        Integer d = (days != null && !days.isEmpty()) ? Integer.parseInt(days) : null;
        Long m = (max != null && !max.isEmpty()) ? Long.parseLong(max) : null;

        return new RetentionConfigDto(d, m);
    }

    public RiskReportDto generateRiskReport(User user) {
        List<RiskItemDto> risks = new ArrayList<>();

        // POPRAWKA: Pobieramy bramki własne ORAZ udostępnione
        List<Gateway> userGateways = gatewayRepository.findAllOwnedAndShared(user.getId());

        for (Gateway gw : userGateways) {
            if (gw.getSensors() == null) continue;

            for (Sensor sensor : gw.getSensors()) {
                if (!sensor.isReportingEnabled()) continue;

                Optional<SensorReading> lastReadingOpt = sensorReadingRepository.findTopBySensorOrderByTimestampDesc(sensor);

                if (lastReadingOpt.isPresent()) {
                    SensorReading reading = lastReadingOpt.get();
                    String valStr = reading.getValue();
                    String type = sensor.getType() != null ? sensor.getType().toUpperCase() : "";
                    String name = sensor.getName();

                    try {
                        if (type.contains("LIGHT") || type.contains("SWIATLO")) {
                            double val = Double.parseDouble(valStr);
                            if (val > 0.5) {
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

                    } catch (NumberFormatException e) {
                        // Ignoruj błędy parsowania
                    }
                }
            }
        }

        boolean isSafe = risks.isEmpty();
        return new RiskReportDto(isSafe, risks.size(), risks);
    }

    private void saveSetting(String key, String value) {
        systemSettingRepository.save(new SystemSetting(key, value));
    }

    private String getSettingValue(String key) {
        return systemSettingRepository.findById(key).map(SystemSetting::getValue).orElse(null);
    }

    public record RetentionConfigDto(Integer retentionDays, Long maxRecords) {}
}

