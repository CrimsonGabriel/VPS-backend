package com.bazunia.vps.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "user_gateway_permissions")
public class UserGatewayPermission {

    @EmbeddedId
    private UserGatewayPermissionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gatewayId")
    @JoinColumn(name = "gateway_id")
    private Gateway gateway;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false)
    private PermissionLevel permissionLevel;
}