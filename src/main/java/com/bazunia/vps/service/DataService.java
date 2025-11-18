package com.bazunia.vps.service;

import com.bazunia.vps.dto.GatewayDto;
import com.bazunia.vps.dto.GatewayUpdateRequest;
import com.bazunia.vps.model.Gateway; // <<< Typ Encji
import com.bazunia.vps.model.User;
import com.bazunia.vps.repository.GatewayRepository;
import com.bazunia.vps.repository.SensorReadingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bazunia.vps.dto.SensorDto;
import com.bazunia.vps.dto.SensorUpdateRequest;
import com.bazunia.vps.dto.SensorConfigDto;
import com.bazunia.vps.model.Sensor; // <<< Typ Encji
import com.bazunia.vps.repository.SensorRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.bazunia.vps.dto.SensorStatusErrorDto;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
@Service
public class DataService {

    private final SensorReadingRepository sensorReadingRepository;
    private final GatewayRepository gatewayRepository;
    private final SensorRepository sensorRepository;

    @Autowired
    public DataService(SensorReadingRepository sensorReadingRepository, GatewayRepository gatewayRepository, SensorRepository sensorRepository) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.gatewayRepository = gatewayRepository;
        this.sensorRepository = sensorRepository;
    }

    @Transactional
    public void deleteAllSensorReadings() {
        sensorReadingRepository.deleteAll();
    }

    @Transactional(readOnly = true)
    public List<GatewayDto> getGatewaysForUser(User user) {
        List<Gateway> gateways = gatewayRepository.findWithSensorsByOwner(user);
        // Ta metoda jest OK, bo służy tylko do CZYTANIA
        return gateways.stream()
                .map(GatewayDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Wymaganie 2.2, 2.3: Aktualizuje bramkę (nazwa, opis, folder).
     * <<< POPRAWKA: Zwraca Encję 'Gateway', a nie 'GatewayDto' >>>
     */
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

        // <<< POPRAWKA: Zwracamy bezpośrednio zapisaną Encję >>>
        return gatewayRepository.save(gateway);
    }

    /**
     * NOWA METODA: Aktualizuje czujnik (nazwa, opis, interwał).
     */
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

        // Weryfikacja właściciela
        if (!Objects.equals(gateway.getOwner().getId(), user.getId())) {
            throw new AccessDeniedException("Brak uprawnień do modyfikacji tej bramki.");
        }

        // <<< GŁÓWNA ZMIANA: Ustawiamy właściciela na null zamiast usuwać >>>
        gateway.setOwner(null);
        gatewayRepository.save(gateway);
    }

    @Transactional(readOnly = true)
    public List<SensorConfigDto> getAllSensorConfigs() {

        return sensorRepository.findAll().stream()
                .map(SensorConfigDto::fromEntity)
                .collect(Collectors.toList());
    }
    /**
     * Ustawia globalny interwał dla wszystkich czujników w bazie danych.
     */
    @Transactional
    public int setGlobalSensorInterval(Integer interval) {
        if (interval == null || interval <= 0) {
            // Możemy ustawić null, aby RPi użyło domyślnego, lub ustawić sztywny limit
            // Ustawmy 60 jako bezpieczny fallback, jeśli ktoś poda 0.
            interval = 60;
        }
        return sensorRepository.setGlobalInterval(interval);
    }
    /**
     * ⭐️ NOWA METODA: Przełącza stan raportowania dla czujnika.
     */
    @Transactional
    public Sensor toggleSensorReporting(Long sensorId, User user) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new EntityNotFoundException("Czujnik o ID " + sensorId + " nie znaleziony."));

        // Weryfikacja właściciela
        if (!Objects.equals(sensor.getGateway().getOwner().getId(), user.getId())) {
            throw new AccessDeniedException("Brak uprawnień do edycji tego czujnika.");
        }

        // Przełącz stan
        sensor.setReportingEnabled(!sensor.isReportingEnabled());
        return sensorRepository.save(sensor);
    }
    /**
     * ⭐️ NOWA METODA: Sprawdza statusy bramek dla użytkownika.
     * Wywoływana przez ApiController, aby znaleźć urządzenia offline.
     */
    @Transactional(readOnly = true)
    public List<SensorStatusErrorDto> checkSensorStatuses(User user) {

        // 1. Zdefiniuj próg "offline".
        // Jeśli bramka nie wysłała danych (przez endpoint /data) w ciągu 5 minut,
        // oznaczamy ją jako offline.
        final LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);

        // 2. Przygotuj listę błędów
        List<SensorStatusErrorDto> errors = new ArrayList<>();

        // 3. Pobierz wszystkie bramki i czujniki użytkownika
        // Używamy metody, którą już masz w repozytorium (obsługuje getGatewaysForUser)
        List<Gateway> gateways = gatewayRepository.findWithSensorsByOwner(user);

        // 4. Iteruj i sprawdzaj
        for (Gateway gateway : gateways) {

            LocalDateTime lastSeen = gateway.getLastSeen();

            // 5. Sprawdź warunek błędu (Brak 'lastSeen' LUB jest starsze niż 'cutoff')
            if (lastSeen == null || lastSeen.isBefore(cutoff)) {

                // 6. Stwórz czytelny komunikat
                String lastSeenText;
                if (lastSeen == null) {
                    lastSeenText = "nigdy";
                } else {
                    // Oblicz, ile minut temu to było, dla lepszego komunikatu
                    long minutesAgo = ChronoUnit.MINUTES.between(lastSeen, LocalDateTime.now());
                    if (minutesAgo == 0) {
                        lastSeenText = "ponad 5 min. temu";
                    } else {
                        lastSeenText = "ponad " + minutesAgo + " min. temu";
                    }
                }

                String message = "Bramka '" + gateway.getName() + "' jest offline. Ostatni kontakt: " + lastSeenText + ".";

                // 7. Dodaj błąd do listy (pasujący do DTO)
                errors.add(new SensorStatusErrorDto(
                        gateway.getId(),            // entityId
                        "GATEWAY",                  // entityType
                        gateway.getName(),          // entityName
                        "OFFLINE",                  // errorType
                        message                     // readableMessage
                ));

                // Uwaga: Jeśli bramka jest offline, celowo nie sprawdzamy
                // jej czujników - błąd bramki jest nadrzędny.
            }

            // Przyszła rozbudowa:
            // else {
            //   // Bramka jest ONLINE.
            //   // Tutaj mógłbyś dodać logikę sprawdzania timestampów
            //   // OSTATNIEGO ODCZYTU dla każdego z jej czujników,
            //   // aby wykryć błędy pojedynczych czujników.
            // }
        }

        return errors;
    }
}