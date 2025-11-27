package com.bazunia.vps.controller;

import com.bazunia.vps.dto.*;
import com.bazunia.vps.model.*;
import com.bazunia.vps.repository.*;
import com.bazunia.vps.service.GatewayService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayService gatewayService;
    private final GatewayRepository gatewayRepository;
    private final UserRepository userRepository;
    private final UserGatewayPermissionRepository permissionRepository;

    @GetMapping("/gateways")
    public ResponseEntity<List<GatewayDto>> getGateways(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(gatewayService.getGatewaysForUser(user));
    }

    @PutMapping("/gateways/{id}")
    public ResponseEntity<GatewayDto> updateGateway(
            @PathVariable Long id,
            @RequestBody GatewayUpdateRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(GatewayDto.fromEntity(gatewayService.updateGateway(id, request, user)));
    }

    @DeleteMapping("/gateways/{id}")
    public ResponseEntity<Void> deleteGateway(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        gatewayService.disassociateGateway(id, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/sensors/{id}")
    public ResponseEntity<SensorDto> updateSensor(
            @PathVariable Long id,
            @RequestBody SensorUpdateRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(SensorDto.fromEntity(gatewayService.updateSensor(id, request, user)));
    }

    @PostMapping("/sensors/{id}/toggle-reporting")
    public ResponseEntity<SensorDto> toggleSensorReporting(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(SensorDto.fromEntity(gatewayService.toggleSensorReporting(id, user)));
    }

    @PostMapping("/sensors/interval/global")
    public ResponseEntity<?> setGlobalSensorInterval(@RequestBody Map<String, Integer> request) {
        Integer interval = request.get("interval");
        if (interval == null) return ResponseEntity.badRequest().body("Brak parametru interval");
        int count = gatewayService.setGlobalSensorInterval(interval);
        return ResponseEntity.ok(Map.of("updatedCount", count));
    }

    @GetMapping("/users/available-for-share")
    public ResponseEntity<List<ShareDto.UserPickDto>> getUsersForShare(Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        List<ShareDto.UserPickDto> users = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .filter(u -> u.getRole() != null && "USER".equals(u.getRole().toString()))
                .map(u -> new ShareDto.UserPickDto(u.getId(), u.getEmail()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/gateways/{gatewayId}/shares")
    public ResponseEntity<?> getGatewayShares(@PathVariable Long gatewayId, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Gateway not found"));

        if (!gateway.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only owner can view shares.");
        }
        List<ShareDto.SharedUserDto> shares = permissionRepository.findByGatewayId(gatewayId).stream()
                .map(perm -> new ShareDto.SharedUserDto(
                        perm.getUser().getId(), perm.getUser().getEmail(), perm.getPermissionLevel()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(shares);
    }

    @PostMapping("/gateways/share")
    public ResponseEntity<?> shareGateway(@RequestBody ShareDto.ShareGatewayRequest request, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        Gateway gateway = gatewayRepository.findById(request.getGatewayId())
                .orElseThrow(() -> new EntityNotFoundException("Gateway not found"));

        if (!gateway.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only owner can share gateway.");
        }

        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new EntityNotFoundException("Target user not found"));

        UserGatewayPermissionId permId = new UserGatewayPermissionId(targetUser.getId(), gateway.getId());
        UserGatewayPermission permission = new UserGatewayPermission();
        permission.setId(permId);
        permission.setUser(targetUser);
        permission.setGateway(gateway);
        permission.setPermissionLevel(request.getPermissionLevel());

        permissionRepository.save(permission);
        return ResponseEntity.ok(Map.of("message", "Udostępniono dla " + targetUser.getEmail()));
    }

    @DeleteMapping("/gateways/{gatewayId}/share/{userId}")
    public ResponseEntity<?> removeShare(@PathVariable Long gatewayId, @PathVariable Long userId, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        Gateway gateway = gatewayRepository.findById(gatewayId)
                .orElseThrow(() -> new EntityNotFoundException("Gateway not found"));

        if (!gateway.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only owner can remove shares.");
        }

        UserGatewayPermission permission = permissionRepository.findByGatewayIdAndUserId(gatewayId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Udostępnienie nie istnieje"));

        permissionRepository.delete(permission);
        return ResponseEntity.ok(Map.of("message", "Access removed."));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}