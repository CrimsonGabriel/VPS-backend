package com.bazunia.vps.service;

import com.bazunia.vps.dto.FolderCreateRequest;
import com.bazunia.vps.dto.FolderDto;
import com.bazunia.vps.dto.GatewayDto;
import com.bazunia.vps.dto.SensorDto;
import com.bazunia.vps.model.*;
import com.bazunia.vps.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final GatewayRepository gatewayRepository;
    private final SensorRepository sensorRepository;
    private final UserGatewayPermissionRepository permissionRepository;
    private final UserFavoriteGatewayRepository favoriteGatewayRepository;
    private final UserFavoriteSensorRepository favoriteSensorRepository;

    public List<FolderDto> getFoldersForUser(User user) {
        return folderRepository.findByOwnerWithGatewaysAndSensors(user).stream()
                .map(FolderDto::fromEntity)
                .collect(Collectors.toList());
    }

    public FolderDto createFolder(FolderCreateRequest request, User user) {
        Folder folder = new Folder(request.name(), request.color(), user);
        Folder savedFolder = folderRepository.save(folder);
        return FolderDto.fromEntity(savedFolder);
    }

    public FolderDto updateFolder(Long folderId, FolderCreateRequest request, User user) {
        Folder folder = folderRepository.findByIdAndOwner(folderId, user)
                .orElseThrow(() -> new EntityNotFoundException("Folder not found"));

        folder.setName(request.name());
        folder.setColor(request.color());
        return FolderDto.fromEntity(folderRepository.save(folder));
    }

    public void deleteFolder(Long folderId, User user) {
        Folder folder = folderRepository.findByIdAndOwner(folderId, user)
                .orElseThrow(() -> new EntityNotFoundException("Folder not found"));
        folderRepository.delete(folder);
    }

    public void addGatewayToFolder(Long folderId, Long gatewayId, User user) {
        Folder folder = folderRepository.findByIdAndOwnerWithGatewaysAndSensors(folderId, user)
                .orElseThrow(() -> new EntityNotFoundException("Folder not found"));

        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Gateway not found"));

        validateAccessToGateway(user, gateway);

        folder.getGateways().add(gateway);
        folderRepository.save(folder);
    }

    public void removeGatewayFromFolder(Long folderId, Long gatewayId, User user) {
        Folder folder = folderRepository.findByIdAndOwnerWithGatewaysAndSensors(folderId, user)
                .orElseThrow(() -> new EntityNotFoundException("Folder not found"));

        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Gateway not found"));

        folder.getGateways().remove(gateway);
        folderRepository.save(folder);
    }

    public void addSensorToFolder(Long folderId, Long sensorId, User user) {
        Folder folder = folderRepository.findByIdAndOwnerWithGatewaysAndSensors(folderId, user)
                .orElseThrow(() -> new EntityNotFoundException("Folder not found"));

        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new EntityNotFoundException("Sensor not found"));

        validateAccessToGateway(user, sensor.getGateway());

        folder.getSensors().add(sensor);
        folderRepository.save(folder);
    }

    public void removeSensorFromFolder(Long folderId, Long sensorId, User user) {
        Folder folder = folderRepository.findByIdAndOwnerWithGatewaysAndSensors(folderId, user)
                .orElseThrow(() -> new EntityNotFoundException("Folder not found"));

        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new EntityNotFoundException("Sensor not found"));

        folder.getSensors().remove(sensor);
        folderRepository.save(folder);
    }

    public void addFavoriteGateway(Long gatewayId, User user) {
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Gateway not found"));

        validateAccessToGateway(user, gateway);

        UserFavoriteGateway favorite = new UserFavoriteGateway(user, gateway);
        favoriteGatewayRepository.save(favorite);
    }

    public void removeFavoriteGateway(Long gatewayId, User user) {
        favoriteGatewayRepository.deleteByUserAndGatewayId(user, gatewayId);
    }

    public List<GatewayDto> getFavoriteGateways(User user) {
        return favoriteGatewayRepository.findByUserWithGateway(user).stream()
                .map(fav -> GatewayDto.fromEntity(fav.getGateway()))
                .collect(Collectors.toList());
    }

    public void addFavoriteSensor(Long sensorId, User user) {
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new EntityNotFoundException("Sensor not found"));

        validateAccessToGateway(user, sensor.getGateway());

        UserFavoriteSensor favorite = new UserFavoriteSensor(user, sensor);
        favoriteSensorRepository.save(favorite);
    }

    public void removeFavoriteSensor(Long sensorId, User user) {
        favoriteSensorRepository.deleteByUserAndSensorId(user, sensorId);
    }

    public List<SensorDto> getFavoriteSensors(User user) {
        return favoriteSensorRepository.findByUserWithSensorAndGateway(user).stream()
                .map(fav -> SensorDto.fromEntity(fav.getSensor()))
                .collect(Collectors.toList());
    }

    private void validateAccessToGateway(User user, Gateway gateway) {
        if (gateway.getOwner().getId().equals(user.getId())) {
            return;
        }

        boolean hasPermission = permissionRepository
                .findByGatewayIdAndUserId(gateway.getId(), user.getId())
                .isPresent();

        if (!hasPermission) {
            throw new AccessDeniedException("Brak dostępu do bramki (nie jesteś właścicielem ani nie została udostępniona).");
        }
    }
}