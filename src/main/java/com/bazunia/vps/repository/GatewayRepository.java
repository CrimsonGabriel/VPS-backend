package com.bazunia.vps.repository;

import com.bazunia.vps.model.Gateway;
import com.bazunia.vps.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface GatewayRepository extends JpaRepository<Gateway, Long> {
    @Query("SELECT g FROM Gateway g LEFT JOIN FETCH g.sensors WHERE g.owner = :user")
    List<Gateway> findWithSensorsByOwner(User user);

    @Query("SELECT DISTINCT g FROM Gateway g " +
            "LEFT JOIN FETCH g.sensors " +
            "LEFT JOIN g.permissions p " +
            "WHERE g.owner.id = :userId " +
            "OR p.user.id = :userId")
    List<Gateway> findAllOwnedAndShared(@Param("userId") Long userId);


}