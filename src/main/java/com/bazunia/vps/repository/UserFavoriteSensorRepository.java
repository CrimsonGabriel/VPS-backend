package com.bazunia.vps.repository;

import com.bazunia.vps.model.User;
import com.bazunia.vps.model.UserFavoriteSensor;
import com.bazunia.vps.model.UserSensorFavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface UserFavoriteSensorRepository extends JpaRepository<UserFavoriteSensor, UserSensorFavoriteId> {

    @Query("SELECT ufs FROM UserFavoriteSensor ufs JOIN FETCH ufs.sensor s JOIN FETCH s.gateway WHERE ufs.user = :user")
    List<UserFavoriteSensor> findByUserWithSensorAndGateway(User user);
    void deleteByUserAndSensorId(User user, Long sensorId);
}