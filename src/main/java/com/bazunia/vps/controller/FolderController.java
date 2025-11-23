package com.bazunia.vps.controller;

import com.bazunia.vps.dto.*;
import com.bazunia.vps.model.User;
import com.bazunia.vps.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    private User getAuthenticatedUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    // --- FOLDERY ---

    @GetMapping("/folders")
    public ResponseEntity<List<FolderDto>> getFolders(Authentication authentication) {
        return ResponseEntity.ok(folderService.getFoldersForUser(getAuthenticatedUser(authentication)));
    }

    @PostMapping("/folders")
    public ResponseEntity<FolderDto> createFolder(@RequestBody FolderCreateRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(folderService.createFolder(request, getAuthenticatedUser(authentication)));
    }

    @PutMapping("/folders/{id}")
    public ResponseEntity<FolderDto> updateFolder(@PathVariable Long id, @RequestBody FolderCreateRequest request, Authentication authentication) {
        return ResponseEntity.ok(folderService.updateFolder(id, request, getAuthenticatedUser(authentication)));
    }

    @DeleteMapping("/folders/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long id, Authentication authentication) {
        folderService.deleteFolder(id, getAuthenticatedUser(authentication));
        return ResponseEntity.noContent().build();
    }

    // --- ZAWARTOŚĆ FOLDERÓW ---

    @PostMapping("/folders/{folderId}/gateways")
    public ResponseEntity<Void> addGatewayToFolder(@PathVariable Long folderId, @RequestBody FolderMembershipRequest request, Authentication authentication) {
        folderService.addGatewayToFolder(folderId, request.gatewayId(), getAuthenticatedUser(authentication));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/folders/{folderId}/gateways/{gatewayId}")
    public ResponseEntity<Void> removeGatewayFromFolder(@PathVariable Long folderId, @PathVariable Long gatewayId, Authentication authentication) {
        folderService.removeGatewayFromFolder(folderId, gatewayId, getAuthenticatedUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/folders/{folderId}/sensors")
    public ResponseEntity<Void> addSensorToFolder(@PathVariable Long folderId, @RequestBody FavoriteRequest request, Authentication authentication) {
        folderService.addSensorToFolder(folderId, request.id(), getAuthenticatedUser(authentication));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/folders/{folderId}/sensors/{sensorId}")
    public ResponseEntity<Void> removeSensorFromFolder(@PathVariable Long folderId, @PathVariable Long sensorId, Authentication authentication) {
        folderService.removeSensorFromFolder(folderId, sensorId, getAuthenticatedUser(authentication));
        return ResponseEntity.noContent().build();
    }

    // --- ULUBIONE ---

    @GetMapping("/favorites/gateways")
    public ResponseEntity<List<GatewayDto>> getFavoriteGateways(Authentication authentication) {
        return ResponseEntity.ok(folderService.getFavoriteGateways(getAuthenticatedUser(authentication)));
    }

    @PostMapping("/favorites/gateways")
    public ResponseEntity<Void> addFavoriteGateway(@RequestBody FavoriteRequest request, Authentication authentication) {
        folderService.addFavoriteGateway(request.id(), getAuthenticatedUser(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/favorites/gateways/{id}")
    public ResponseEntity<Void> removeFavoriteGateway(@PathVariable Long id, Authentication authentication) {
        folderService.removeFavoriteGateway(id, getAuthenticatedUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/favorites/sensors")
    public ResponseEntity<List<SensorDto>> getFavoriteSensors(Authentication authentication) {
        return ResponseEntity.ok(folderService.getFavoriteSensors(getAuthenticatedUser(authentication)));
    }

    @PostMapping("/favorites/sensors")
    public ResponseEntity<Void> addFavoriteSensor(@RequestBody FavoriteRequest request, Authentication authentication) {
        folderService.addFavoriteSensor(request.id(), getAuthenticatedUser(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/favorites/sensors/{id}")
    public ResponseEntity<Void> removeFavoriteSensor(@PathVariable Long id, Authentication authentication) {
        folderService.removeFavoriteSensor(id, getAuthenticatedUser(authentication));
        return ResponseEntity.noContent().build();
    }
}