package com.bazunia.vps.service;

import com.bazunia.vps.dto.GatewayUpdateRequest;
import com.bazunia.vps.model.Gateway;
import com.bazunia.vps.model.User;
import com.bazunia.vps.repository.GatewayRepository;
import com.bazunia.vps.repository.UserGatewayPermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayServiceTest {

    @Mock
    private GatewayRepository gatewayRepository;

    @Mock
    private UserGatewayPermissionRepository permissionRepository;

    @InjectMocks
    private GatewayService gatewayService;

    @Test
    void updateGateway_ShouldSuccess_WhenUserIsOwner() {
        // ARRANGE
        Long gatewayId = 1L;
        Long userId = 50L;

        User owner = new User();
        owner.setId(userId);

        Gateway gateway = new Gateway();
        gateway.setId(gatewayId);
        gateway.setOwner(owner);
        gateway.setName("Old Name");

        GatewayUpdateRequest request = new GatewayUpdateRequest(
                "New Name",
                "New Desc",
                "New Folder"
        );

        when(gatewayRepository.findById(gatewayId)).thenReturn(Optional.of(gateway));
        when(gatewayRepository.save(any(Gateway.class))).thenAnswer(i -> i.getArgument(0));

        // ACT
        Gateway updated = gatewayService.updateGateway(gatewayId, request, owner);

        // ASSERT
        assertEquals("New Name", updated.getName());
        assertEquals("New Desc", updated.getDescription());
        verify(gatewayRepository).save(gateway);
    }

    @Test
    void updateGateway_ShouldThrowAccessDenied_WhenUserIsNotOwnerAndNoPermission() {
        // ARRANGE
        Long gatewayId = 1L;
        Long ownerId = 50L;
        Long hackerId = 666L;

        User owner = new User();
        owner.setId(ownerId);

        User hacker = new User();
        hacker.setId(hackerId);

        Gateway gateway = new Gateway();
        gateway.setId(gatewayId);
        gateway.setOwner(owner);

        GatewayUpdateRequest request = new GatewayUpdateRequest("Hacked Name", null, null);

        when(gatewayRepository.findById(gatewayId)).thenReturn(Optional.of(gateway));
        when(permissionRepository.findByGatewayIdAndUserId(gatewayId, hackerId))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> gatewayService.updateGateway(gatewayId, request, hacker));

        verify(gatewayRepository, never()).save(any());
    }
}