package com.bazunia.vps.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserGatewayFavoriteId implements Serializable {
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "gateway_id")
    private Long gatewayId;

    public UserGatewayFavoriteId() {}
    public UserGatewayFavoriteId(Long userId, Long gatewayId) {
        this.userId = userId;
        this.gatewayId = gatewayId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserGatewayFavoriteId that = (UserGatewayFavoriteId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(gatewayId, that.gatewayId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, gatewayId);
    }
}