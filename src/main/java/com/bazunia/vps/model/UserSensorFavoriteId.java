package com.bazunia.vps.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserSensorFavoriteId implements Serializable {
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "sensor_id")
    private Long sensorId;

    // Konstruktory, equals() i hashCode() są niezbędne
    public UserSensorFavoriteId() {}
    public UserSensorFavoriteId(Long userId, Long sensorId) {
        this.userId = userId;
        this.sensorId = sensorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSensorFavoriteId that = (UserSensorFavoriteId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(sensorId, that.sensorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, sensorId);
    }
}