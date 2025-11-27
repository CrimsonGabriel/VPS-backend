package com.bazunia.vps.service;

import com.bazunia.vps.dto.SensorReadingResponseDto;
import com.bazunia.vps.model.SystemSetting;
import com.bazunia.vps.repository.SensorReadingRepository;
import com.bazunia.vps.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RetentionService {

    private static final Logger log = LoggerFactory.getLogger(RetentionService.class);

    private final SensorReadingRepository sensorReadingRepository;
    private final SystemSettingRepository systemSettingRepository;

    public record RetentionConfigDto(Integer retentionDays, Long maxRecords) {}

    @Transactional
    public void deleteAllSensorReadings() {
        sensorReadingRepository.deleteAll();
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void performAutoCleanup() {
        log.info("Rozpoczynanie automatycznego czyszczenia historii...");
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
                    log.info("Usunięto rekordy starsze niż {} dni.", days);
                }
            } catch (NumberFormatException e) {
                log.error("Błąd formatu RETENTION_DAYS: {}", daysStr);
            }
        }

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
            log.info("Przycięto tabelę do {} rekordów.", limit);
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

    private void saveSetting(String key, String value) {
        systemSettingRepository.save(new SystemSetting(key, value));
    }

    private String getSettingValue(String key) {
        return systemSettingRepository.findById(key).map(SystemSetting::getValue).orElse(null);
    }
}