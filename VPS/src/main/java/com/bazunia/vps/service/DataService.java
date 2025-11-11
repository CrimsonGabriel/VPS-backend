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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
}