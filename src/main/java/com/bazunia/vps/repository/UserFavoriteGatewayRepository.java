package com.bazunia.vps.repository;

import com.bazunia.vps.model.User;
import com.bazunia.vps.model.UserFavoriteGateway;
import com.bazunia.vps.model.UserGatewayFavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface UserFavoriteGatewayRepository extends JpaRepository<UserFavoriteGateway, UserGatewayFavoriteId> {

    @Query("SELECT ufg FROM UserFavoriteGateway ufg JOIN FETCH ufg.gateway WHERE ufg.user = :user")
    List<UserFavoriteGateway> findByUserWithGateway(User user);
    void deleteByUserAndGatewayId(User user, Long gatewayId);
}