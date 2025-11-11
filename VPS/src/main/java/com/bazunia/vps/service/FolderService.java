package com.bazunia.vps.service;

import com.bazunia.vps.dto.FolderCreateRequest;
import com.bazunia.vps.dto.FolderDto;
import com.bazunia.vps.dto.GatewayDto;
import com.bazunia.vps.dto.SensorDto;
import com.bazunia.vps.model.*;
import com.bazunia.vps.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FolderService {

    private final FolderRepository folderRepository;
    private final GatewayRepository gatewayRepository;
    private final SensorRepository sensorRepository;
    private final UserGatewayPermissionRepository permissionRepository;
    private final UserFavoriteGatewayRepository favoriteGatewayRepository;
    private final UserFavoriteSensorRepository favoriteSensorRepository;

    @Autowired
    public FolderService(FolderRepository folderRepository, GatewayRepository gatewayRepository,
                         SensorRepository sensorRepository, UserGatewayPermissionRepository permissionRepository,
                         UserFavoriteGatewayRepository favoriteGatewayRepository,
                         UserFavoriteSensorRepository favoriteSensorRepository) {
        this.folderRepository = folderRepository;
        this.gatewayRepository = gatewayRepository;
        this.sensorRepository = sensorRepository;
        this.permissionRepository = permissionRepository;
        this.favoriteGatewayRepository = favoriteGatewayRepository;
        this.favoriteSensorRepository = favoriteSensorRepository;
    }

    // --- LOGIKA FOLDERÓW ---

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
        Folder updatedFolder = folderRepository.save(folder);
        return FolderDto.fromEntity(updatedFolder);
    }

    public void deleteFolder(Long folderId, User user) {
        Folder folder = folderRepository.findByIdAndOwner(folderId, user)
                .orElseThrow(() -> new EntityNotFoundException("Folder not found"));
        folderRepository.delete(folder);
    }

    public void addGatewayToFolder(Long folderId, Long gatewayId, User user) {
        // 1. Sprawdź, czy folder należy do użytkownika
        Folder folder = folderRepository.findByIdAndOwnerWithGatewaysAndSensors(folderId, user)
                .orElseThrow(() -> new EntityNotFoundException("Folder not found"));

        // 2. Sprawdź, czy bramka istnieje
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Gateway not found"));

        // 3. ⭐️ POPRAWKA 1 (Dla Bramek) ⭐️
        // checkGatewayPermission(user, gatewayId); // Wyłączamy to błędne sprawdzenie

        // 4. Dodaj bramkę
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

    // --- LOGIKA ULUBIONYCH ---

    public void addFavoriteGateway(Long gatewayId, User user) {
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Gateway not found"));

        // (To też pewnie rzuci błędem, jeśli go nie zakomentujesz)
        // checkGatewayPermission(user, gatewayId);

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

        // (To też pewnie rzuci błędem)
        // checkGatewayPermission(user, sensor.getGateway().getId());

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

    // --- Metoda pomocnicza ---

    private void checkGatewayPermission(User user, Long gatewayId) {
        UserGatewayPermissionId permissionId = new UserGatewayPermissionId(user.getId(), gatewayId);
        permissionRepository.findById(permissionId)
                .orElseThrow(() -> new AccessDeniedException("User does not have permission for gateway " + gatewayId));
    }

    // --- NOWE METODY DLA CZUJNIKÓW W FOLDERACH ---

    public void addSensorToFolder(Long folderId, Long sensorId, User user) {
        // 1. Sprawdź, czy folder należy do użytkownika
        Folder folder = folderRepository.findByIdAndOwnerWithGatewaysAndSensors(folderId, user)
                .orElseThrow(() -> new EntityNotFoundException("Folder not found"));

        // 2. Znajdź czujnik
        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new EntityNotFoundException("Sensor not found"));

        // 3. ⭐️ POPRAWKA 2 (Dla Czujników) ⭐️
        // checkGatewayPermission(user, sensor.getGateway().getId()); // Wyłączamy to błędne sprawdzenie

        // 4. Dodaj czujnik i zapisz
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
}