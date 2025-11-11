package com.bazunia.vps.controller;

import com.bazunia.vps.dto.*;
import com.bazunia.vps.model.User;
import com.bazunia.vps.service.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api") // Wszystkie endpointy będą pod /api
public class FolderController {

    private final FolderService folderService;

    @Autowired
    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    /**
     * Metoda pomocnicza do pobierania użytkownika z kontekstu Spring Security
     */
    private User getAuthenticatedUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    // --- ENDPOINTY DLA FOLDERÓW ---

    @GetMapping("/folders")
    public ResponseEntity<List<FolderDto>> getFolders(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        List<FolderDto> folders = folderService.getFoldersForUser(user);
        return ResponseEntity.ok(folders);
    }

    @PostMapping("/folders")
    public ResponseEntity<FolderDto> createFolder(@RequestBody FolderCreateRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        FolderDto newFolder = folderService.createFolder(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(newFolder);
    }

    @PutMapping("/folders/{id}")
    public ResponseEntity<FolderDto> updateFolder(@PathVariable Long id, @RequestBody FolderCreateRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        FolderDto updatedFolder = folderService.updateFolder(id, request, user);
        return ResponseEntity.ok(updatedFolder);
    }

    @DeleteMapping("/folders/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        folderService.deleteFolder(id, user);
        return ResponseEntity.noContent().build();
    }

    // --- ENDPOINTY DLA ZAWARTOŚCI FOLDERÓW ---

    @PostMapping("/folders/{folderId}/gateways")
    public ResponseEntity<Void> addGatewayToFolder(@PathVariable Long folderId, @RequestBody FolderMembershipRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        folderService.addGatewayToFolder(folderId, request.gatewayId(), user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/folders/{folderId}/gateways/{gatewayId}")
    public ResponseEntity<Void> removeGatewayFromFolder(@PathVariable Long folderId, @PathVariable Long gatewayId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        folderService.removeGatewayFromFolder(folderId, gatewayId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/folders/{folderId}/sensors")
    public ResponseEntity<Void> addSensorToFolder(@PathVariable Long folderId, @RequestBody FavoriteRequest request, Authentication authentication) {
        // Używamy FavoriteRequest, bo pasuje (tylko "id")
        User user = getAuthenticatedUser(authentication);
        folderService.addSensorToFolder(folderId, request.id(), user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/folders/{folderId}/sensors/{sensorId}")
    public ResponseEntity<Void> removeSensorFromFolder(@PathVariable Long folderId, @PathVariable Long sensorId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        folderService.removeSensorFromFolder(folderId, sensorId, user);
        return ResponseEntity.noContent().build();
    }

    // --- ENDPOINTY DLA ULUBIONYCH ---

    @GetMapping("/favorites/gateways")
    public ResponseEntity<List<GatewayDto>> getFavoriteGateways(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        List<GatewayDto> gateways = folderService.getFavoriteGateways(user);
        return ResponseEntity.ok(gateways);
    }

    @PostMapping("/favorites/gateways")
    public ResponseEntity<Void> addFavoriteGateway(@RequestBody FavoriteRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        folderService.addFavoriteGateway(request.id(), user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/favorites/gateways/{id}")
    public ResponseEntity<Void> removeFavoriteGateway(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        folderService.removeFavoriteGateway(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/favorites/sensors")
    public ResponseEntity<List<SensorDto>> getFavoriteSensors(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        List<SensorDto> sensors = folderService.getFavoriteSensors(user);
        return ResponseEntity.ok(sensors);
    }

    @PostMapping("/favorites/sensors")
    public ResponseEntity<Void> addFavoriteSensor(@RequestBody FavoriteRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        folderService.addFavoriteSensor(request.id(), user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/favorites/sensors/{id}")
    public ResponseEntity<Void> removeFavoriteSensor(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        folderService.removeFavoriteSensor(id, user);
        return ResponseEntity.noContent().build();
    }
}