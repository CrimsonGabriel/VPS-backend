package com.bazunia.vps.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_favorite_gateways")
public class UserFavoriteGateway {

    @EmbeddedId
    private UserGatewayFavoriteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gatewayId")
    @JoinColumn(name = "gateway_id")
    private Gateway gateway;

    @Column(name = "favorited_at", nullable = false)
    private LocalDateTime favoritedAt;

    public UserFavoriteGateway(User user, Gateway gateway) {
        this.id = new UserGatewayFavoriteId(user.getId(), gateway.getId());
        this.user = user;
        this.gateway = gateway;
        this.favoritedAt = LocalDateTime.now();
    }
}