package com.bazunia.vps.repository;

import com.bazunia.vps.model.Gateway;
import com.bazunia.vps.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface GatewayRepository extends JpaRepository<Gateway, Long> {
    @Query("SELECT g FROM Gateway g LEFT JOIN FETCH g.sensors WHERE g.owner = :user")
    List<Gateway> findWithSensorsByOwner(User user);
    List<Gateway> findAllByOwner(User owner);


}