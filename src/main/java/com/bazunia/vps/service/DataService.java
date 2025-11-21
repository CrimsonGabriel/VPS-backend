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
@Service
public class DataService {

    private static final Logger log = LoggerFactory.getLogger(DataService.class);

    private final SensorReadingRepository sensorReadingRepository;
    private final GatewayRepository gatewayRepository;
    private final SensorRepository sensorRepository;
    private final SystemSettingRepository systemSettingRepository;

    @Autowired
    public DataService(SensorReadingRepository sensorReadingRepository,
                       GatewayRepository gatewayRepository,
                       SensorRepository sensorRepository,
                       SystemSettingRepository systemSettingRepository) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.gatewayRepository = gatewayRepository;
        this.sensorRepository = sensorRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    @Transactional
    public void deleteAllSensorReadings() {
        sensorReadingRepository.deleteAll();
    }

    @Transactional(readOnly = true)
    public List<GatewayDto> getGatewaysForUser(User user) {
        List<Gateway> gateways = gatewayRepository.findWithSensorsByOwner(user);
        return gateways.stream()
                .map(GatewayDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public Gateway updateGateway(Long gatewayId, GatewayUpdateRequest request, User user) {
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Bramka o ID " + gatewayId + " nie znaleziona."));

        if (!Objects.equals(gateway.getOwner().getId(), user.getId())) {
            throw new AccessDeniedException("Brak uprawnień do edycji tej bramki.");
        }

        gateway.setName(request.name());
        gateway.setDescription(request.description());
        gateway.setFolder(request.folder());

        return gatewayRepository.save(gateway);
    }

    @Transactional
    public Sensor updateSensor(Long sensorId, SensorUpdateRequest request, User user) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new EntityNotFoundException("Czujnik o ID " + sensorId + " nie znaleziony."));

        if (!Objects.equals(sensor.getGateway().getOwner().getId(), user.getId())) {
            throw new AccessDeniedException("Brak uprawnień do edycji tego czujnika.");
        }

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

    @Transactional
    public void disassociateGateway(Long gatewayId, User user) {
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Bramka o ID " + gatewayId + " nie znaleziona."));

        if (!Objects.equals(gateway.getOwner().getId(), user.getId())) {
            throw new AccessDeniedException("Brak uprawnień do modyfikacji tej bramki.");
        }

        gateway.setOwner(null);
        gatewayRepository.save(gateway);
    }

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

    @Transactional
    public Sensor toggleSensorReporting(Long sensorId, User user) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new EntityNotFoundException("Czujnik o ID " + sensorId + " nie znaleziony."));

        if (!Objects.equals(sensor.getGateway().getOwner().getId(), user.getId())) {
            throw new AccessDeniedException("Brak uprawnień do edycji tego czujnika.");
        }

        sensor.setReportingEnabled(!sensor.isReportingEnabled());
        return sensorRepository.save(sensor);
    }

    @Transactional(readOnly = true)
    public List<SensorStatusErrorDto> checkSensorStatuses(User user) {
        final LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        List<SensorStatusErrorDto> errors = new ArrayList<>();
        List<Gateway> gateways = gatewayRepository.findWithSensorsByOwner(user);

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
                    // Zamieniamy dni na milisekundy (long), bo tak jest w bazie
                    long cutoffMillis = LocalDateTime.now()
                            .minusDays(days)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli();

                    // Wywołujemy metodę repozytorium przyjmującą long
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

    // Pomocnicza metoda: Usuwa nadmiarowe rekordy, zostawiając 'limit' najnowszych
    private void trimToSize(long limit) {
        long count = sensorReadingRepository.count();
        if (count <= limit) return;

        var pageRequest = PageRequest.of((int) limit, 1, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<SensorReadingResponseDto> result = sensorReadingRepository.findLatestReadingsWithDetails(pageRequest);

        if (!result.isEmpty()) {
            // DTO zwraca teraz czysty 'long', więc nie musimy nic konwertować na LocalDateTime
            long cutoffTimestamp = result.get(0).timestamp();

            // Usuwamy starsze rekordy (mniejszy timestamp)
            sensorReadingRepository.deleteByTimestampLessThan(cutoffTimestamp);
            log.info("Przycięto tabelę do {} rekordów (limit).", limit);
        }
    }

    // Metody dla AdminController do zarządzania ustawieniami
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

    private void saveSetting(String key, String value) {
        systemSettingRepository.save(new SystemSetting(key, value));
    }

    private String getSettingValue(String key) {
        return systemSettingRepository.findById(key).map(SystemSetting::getValue).orElse(null);
    }

    public record RetentionConfigDto(Integer retentionDays, Long maxRecords) {
    }

    public RiskReportDto generateRiskReport(User user) {
        List<RiskItemDto> risks = new ArrayList<>();

        // Używamy metody findWithSensorsByOwner, bo od razu pobiera sensory (wydajniej)
        // Zastępuje to błędne findAllByUser
        List<Gateway> userGateways = gatewayRepository.findWithSensorsByOwner(user);

        for (Gateway gw : userGateways) {
            if (gw.getSensors() == null) continue;

            for (Sensor sensor : gw.getSensors()) {
                // Pomiń wyłączone z raportowania
                if (!sensor.isReportingEnabled()) continue;

                // Pobierz OSTATNI odczyt dla czujnika (zwraca Optional)
                Optional<SensorReading> lastReadingOpt = sensorReadingRepository.findTopBySensorOrderByTimestampDesc(sensor);

                if (lastReadingOpt.isPresent()) {
                    // .get() jest teraz bezpieczne, bo sprawdziliśmy isPresent()
                    SensorReading reading = lastReadingOpt.get();
                    String valStr = reading.getValue();

                    String type = sensor.getType() != null ? sensor.getType().toUpperCase() : "";
                    String name = sensor.getName();

                    try {
                        // 1. Logika dla ŚWIATŁA (zakładamy 1.0 = ON)
                        if (type.contains("LIGHT") || type.contains("SWIATLO")) {
                            double val = Double.parseDouble(valStr);
                            if (val > 0.5) {
                                risks.add(new RiskItemDto(name, "Światło włączone", "light"));
                            }
                        }

                        // 2. Logika dla OKNA/DRZWI (zakładamy 0.0 = OTWARTE, jak w kontaktronach)
                        if (type.contains("DOOR") || type.contains("WINDOW") || type.contains("KONTAKTRON")) {
                            // Uwaga: dostosuj warunek do swoich czujników.
                            // Często 1=Zamknięte, 0=Otwarte.
                            if ("0.0".equals(valStr) || "0".equals(valStr)) {
                                risks.add(new RiskItemDto(name, "Otwarte", "window"));
                            }
                        }

                        // 3. Logika PROGÓW (Thresholds) z bazy danych
                        double val = Double.parseDouble(valStr);

                        if (sensor.getAlarmThresholdHigh() != null && val > sensor.getAlarmThresholdHigh()) {
                            risks.add(new RiskItemDto(name, "Przekroczono MAX: " + val, "warning"));
                        }

                        if (sensor.getAlarmThresholdLow() != null && val < sensor.getAlarmThresholdLow()) {
                            risks.add(new RiskItemDto(name, "Poniżej MIN: " + val, "warning"));
                        }

                    } catch (NumberFormatException e) {
                        // Ignorujemy wartości, których nie da się zamienić na liczbę (np. "ERR")
                    }
                }
            }
        }

        boolean isSafe = risks.isEmpty();
        return new RiskReportDto(isSafe, risks.size(), risks);
    }
}

