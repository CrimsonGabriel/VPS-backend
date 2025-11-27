package com.bazunia.vps.service;

import com.bazunia.vps.dto.GatewayDto;
import com.bazunia.vps.dto.GatewayUpdateRequest;
import com.bazunia.vps.dto.SensorConfigDto;
import com.bazunia.vps.dto.SensorUpdateRequest;
import com.bazunia.vps.model.*;
import com.bazunia.vps.repository.GatewayRepository;
import com.bazunia.vps.repository.SensorRepository;
import com.bazunia.vps.repository.UserGatewayPermissionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GatewayService {

    private final GatewayRepository gatewayRepository;
    private final SensorRepository sensorRepository;
    private final UserGatewayPermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<GatewayDto> getGatewaysForUser(User user) {
        return gatewayRepository.findAllOwnedAndShared(user.getId()).stream()
                .map(GatewayDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public Gateway updateGateway(Long gatewayId, GatewayUpdateRequest request, User user) {
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Bramka o ID " + gatewayId + " nie znaleziona."));
        validateWriteAccess(gateway, user);

        gateway.setName(request.name());
        gateway.setDescription(request.description());
        gateway.setFolder(request.folder());
        return gatewayRepository.save(gateway);
    }

    @Transactional
    public void disassociateGateway(Long gatewayId, User user) {
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Bramka o ID " + gatewayId + " nie znaleziona."));
        if (!Objects.equals(gateway.getOwner().getId(), user.getId())) {
            throw new AccessDeniedException("Tylko właściciel może usunąć bramkę.");
        }
        permissionRepository.deleteAllByGatewayId(gatewayId);
        gateway.setOwner(null);
        gatewayRepository.save(gateway);
    }


    @Transactional
    public Sensor updateSensor(Long sensorId, SensorUpdateRequest request, User user) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new EntityNotFoundException("Czujnik o ID " + sensorId + " nie znaleziony."));
        validateWriteAccess(sensor.getGateway(), user);

        if (request.name() != null && !request.name().isEmpty()) sensor.setName(request.name());
        if (request.description() != null) sensor.setDescription(request.description());
        if (request.intervalSeconds() != null) sensor.setIntervalSeconds(request.intervalSeconds());

        return sensorRepository.save(sensor);
    }

    @Transactional
    public Sensor toggleSensorReporting(Long sensorId, User user) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new EntityNotFoundException("Czujnik o ID " + sensorId + " nie znaleziony."));
        validateWriteAccess(sensor.getGateway(), user);
        sensor.setReportingEnabled(!sensor.isReportingEnabled());
        return sensorRepository.save(sensor);
    }

    @Transactional(readOnly = true)
    public List<SensorConfigDto> getAllSensorConfigs() {
        return sensorRepository.findAll().stream()
                .map(SensorConfigDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public int setGlobalSensorInterval(Integer interval) {
        if (interval == null || interval <= 0) interval = 60;
        return sensorRepository.setGlobalInterval(interval);
    }

    private void validateWriteAccess(Gateway gateway, User user) {
        if (Objects.equals(gateway.getOwner().getId(), user.getId())) return;

        Optional<UserGatewayPermission> permission = permissionRepository.findByGatewayIdAndUserId(gateway.getId(), user.getId());
        if (permission.isPresent() && permission.get().getPermissionLevel() == PermissionLevel.FULL_ACCESS) {
            return;
        }
        throw new AccessDeniedException("Brak uprawnień do edycji.");
    }
}