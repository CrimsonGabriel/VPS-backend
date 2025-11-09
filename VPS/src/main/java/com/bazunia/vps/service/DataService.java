package com.bazunia.vps.service;

import com.bazunia.vps.dto.GatewayDto; // <<< DODAJ IMPORT
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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors; // <<< DODAJ IMPORT

@Service
public class DataService {

    // ... (pola i konstruktor bez zmian) ...
    private final SensorReadingRepository sensorReadingRepository;
    private final GatewayRepository gatewayRepository;

    @Autowired
    public DataService(SensorReadingRepository sensorReadingRepository, GatewayRepository gatewayRepository) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.gatewayRepository = gatewayRepository;
    }

    @Transactional
    public void deleteAllSensorReadings() {
        // ... (bez zmian) ...
    }

    // --- NOWA LOGIKA DLA WYMAGAŃ 2.x ---

    /**
     * Wymaganie 2.4: Pobiera wszystkie bramki (wraz z czujnikami) należące do danego użytkownika.
     * <<< POPRAWKA: Zwraca List<GatewayDto> zamiast List<Gateway> >>>
     */
    @Transactional(readOnly = true)
    public List<GatewayDto> getGatewaysForUser(User user) {
        // 1. Pobierz encje z bazy (tak jak wcześniej)
        List<Gateway> gateways = gatewayRepository.findWithSensorsByOwner(user);

        // 2. Skonwertuj encje na DTO (to odłącza je od sesji Hibernate)
        return gateways.stream()
                .map(GatewayDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Wymaganie 2.2, 2.3: Aktualizuje bramkę (nazwa, opis, folder).
     * <<< POPRAWKA: Zwraca GatewayDto >>>
     */
    @Transactional
    public GatewayDto updateGateway(Long gatewayId, GatewayUpdateRequest request, User user) {
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Bramka o ID " + gatewayId + " nie znaleziona."));

        // Weryfikacja właściciela
        if (!Objects.equals(gateway.getOwner().getId(), user.getId())) {
            throw new AccessDeniedException("Brak uprawnień do edycji tej bramki.");
        }

        gateway.setName(request.name());
        gateway.setDescription(request.description());
        gateway.setFolder(request.folder());

        Gateway savedGateway = gatewayRepository.save(gateway);

        // 3. Zwróć DTO, a nie encję
        return GatewayDto.fromEntity(savedGateway);
    }

    // ... (metoda deleteGateway bez zmian) ...
    @Transactional
    public void deleteGateway(Long gatewayId, User user) {
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Bramka o ID " + gatewayId + " nie znaleziona."));

        if (!Objects.equals(gateway.getOwner().getId(), user.getId())) {
            throw new AccessDeniedException("Brak uprawnień do usunięcia tej bramki.");
        }

        gatewayRepository.delete(gateway);
    }
}