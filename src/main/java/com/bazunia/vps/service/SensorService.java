package com.bazunia.vps.service;

import com.bazunia.vps.dto.SensorCreateRequest;
import com.bazunia.vps.dto.SensorUpdateRequest; // <--- Ważny import
import com.bazunia.vps.model.Gateway;
import com.bazunia.vps.model.Sensor;
import com.bazunia.vps.repository.GatewayRepository;
import com.bazunia.vps.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SensorService {

    private final SensorRepository sensorRepository;
    private final GatewayRepository gatewayRepository;

    @Transactional
    public Sensor createSensor(SensorCreateRequest request) {

        // 1. Sprawdź, czy sensor o tym ID już istnieje (Twoje wymaganie)
        if (sensorRepository.existsById(request.getId())) {
            throw new RuntimeException("Sensor o ID " + request.getId() + " już istnieje.");
        }

        // 2. Znajdź bramkę (Gateway)
        Gateway gateway = gatewayRepository.findById(request.getGatewayId())
                .orElseThrow(() -> new RuntimeException("Bramka (Gateway) o ID " + request.getGatewayId() + " nie istnieje."));

        // 3. (OPCJONALNIE, ALE ZALECANE) Sprawdź, czy nazwa jest unikalna w obrębie bramki
        sensorRepository.findByNameAndGatewayId(request.getName(), request.getGatewayId())
                .ifPresent(existingSensor -> {
                    throw new RuntimeException("Sensor o nazwie '" + request.getName() + "' już istnieje dla tej bramki.");
                });

        // 4. Stwórz nowy obiekt Sensor
        Sensor newSensor = new Sensor();

        // Ustaw pola obowiązkowe (teraz ustawiamy ID i createdAt ręcznie)
        newSensor.setId(request.getId());
        newSensor.setCreatedAt(request.getCreatedAt());
        newSensor.setGateway(gateway);
        newSensor.setName(request.getName());
        newSensor.setType(request.getType());

        // Ustaw pola nieobowiązkowe
        newSensor.setDescription(request.getDescription());
        newSensor.setIntervalSeconds(request.getIntervalSeconds());
        newSensor.setAlarmThresholdLow(request.getAlarmThresholdLow());
        newSensor.setAlarmThresholdHigh(request.getAlarmThresholdHigh());
        newSensor.setBatteryLevel(request.getBatteryLevel());
        newSensor.setKeyword(request.getKeyword());

        if (request.getReportingEnabled() != null) {
            newSensor.setReportingEnabled(request.getReportingEnabled());
        }

        // 5. Zapisz sensor w bazie
        return sensorRepository.save(newSensor);
    }

    // ----------------------------------------------------------------------
    // ⭐️⭐️⭐️ NOWA METODA DO AKTUALIZACJI (PATCH) ⭐️⭐️⭐️
    // ----------------------------------------------------------------------

    @Transactional
    public Sensor updateSensor(Long sensorId, SensorUpdateRequest request) {

        // 1. Znajdź sensor, który chcesz zaktualizować
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new RuntimeException("Sensor o ID " + sensorId + " nie istnieje."));

        // 2. Walidacja unikalności nazwy (jeśli nazwa lub bramka się zmienia)
        if (request.name() != null || request.gatewayId() != null) {
            String newName = (request.name() != null) ? request.name() : sensor.getName();
            Long newGatewayId = (request.gatewayId() != null) ? request.gatewayId() : sensor.getGateway().getId();

            sensorRepository.findByNameAndGatewayId(newName, newGatewayId)
                    .ifPresent(existingSensor -> {
                        // Upewnij się, że znaleziony sensor to nie ten sam, który edytujemy
                        if (!existingSensor.getId().equals(sensorId)) {
                            throw new RuntimeException("Sensor o nazwie '" + newName + "' już istnieje dla tej bramki.");
                        }
                    });
        }

        // 3. Aktualizuj pola, jeśli nie są 'null' w żądaniu (logika PATCH)

        // Zmiana bramki (Gateway)
        if (request.gatewayId() != null) {
            Gateway newGateway = gatewayRepository.findById(request.gatewayId())
                    .orElseThrow(() -> new RuntimeException("Bramka (Gateway) o ID " + request.gatewayId() + " nie istnieje."));
            sensor.setGateway(newGateway);
        }

        // Pozostałe pola z DTO
        if (request.name() != null) {
            sensor.setName(request.name());
        }
        if (request.type() != null) {
            sensor.setType(request.type());
        }
        if (request.createdAt() != null) {
            sensor.setCreatedAt(request.createdAt());
        }
        if (request.description() != null) {
            sensor.setDescription(request.description());
        }
        if (request.intervalSeconds() != null) {
            sensor.setIntervalSeconds(request.intervalSeconds());
        }
        if (request.alarmThresholdLow() != null) {
            sensor.setAlarmThresholdLow(request.alarmThresholdLow());
        }
        if (request.alarmThresholdHigh() != null) {
            sensor.setAlarmThresholdHigh(request.alarmThresholdHigh());
        }
        if (request.batteryLevel() != null) {
            sensor.setBatteryLevel(request.batteryLevel());
        }
        if (request.keyword() != null) {
            sensor.setKeyword(request.keyword());
        }
        if (request.reportingEnabled() != null) {
            sensor.setReportingEnabled(request.reportingEnabled());
        }

        // 4. Zapisz zmiany do bazy
        return sensorRepository.save(sensor);
    }
}