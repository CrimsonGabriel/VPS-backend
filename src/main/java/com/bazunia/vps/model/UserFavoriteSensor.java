package com.bazunia.vps.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_favorite_sensors")
public class UserFavoriteSensor {

    @EmbeddedId
    private UserSensorFavoriteId id; // Teraz poprawnie odnosi się do publicznej klasy

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sensorId")
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    @Column(name = "favorited_at", nullable = false)
    private LocalDateTime favoritedAt;

    public UserFavoriteSensor(User user, Sensor sensor) {
        this.id = new UserSensorFavoriteId(user.getId(), sensor.getId());
        this.user = user;
        this.sensor = sensor;
        this.favoritedAt = LocalDateTime.now();
    }
}