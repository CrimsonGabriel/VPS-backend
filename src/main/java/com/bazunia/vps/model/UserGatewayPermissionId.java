package com.bazunia.vps.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserGatewayPermissionId implements Serializable {
    private Long userId;
    private Long gatewayId;
}