package com.bazunia.vps.repository;

import com.bazunia.vps.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
    /**
     * Ustawia ten sam interwał dla wszystkich czujników.
     * Używamy @Modifying, ponieważ jest to operacja UPDATE.
     */
    @Modifying
    @Query("UPDATE Sensor s SET s.intervalSeconds = :interval")
    int setGlobalInterval(@Param("interval") Integer interval);
    Optional<Sensor> findByNameAndGatewayId(String name, Long gatewayId);
}