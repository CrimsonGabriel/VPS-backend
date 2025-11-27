package com.bazunia.vps.repository;

import com.bazunia.vps.model.SensorReading;
import com.bazunia.vps.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bazunia.vps.dto.SensorReadingResponseDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {


    @Query("SELECT new com.bazunia.vps.dto.SensorReadingResponseDto(" +
            "r.gateway.id, r.sensor.id, r.sensor.type, r.value, r.timestamp, r.id) " +
            "FROM SensorReading r " +
            "ORDER BY r.timestamp DESC")
    List<SensorReadingResponseDto> findLatestReadingsWithDetails(Pageable pageable);

    /**
     * Usuwa rekordy starsze niż podany timestamp.
     */
    @Modifying
    @Query("DELETE FROM SensorReading r WHERE r.timestamp < :timestampLimit")
    void deleteByTimestampLessThan(@Param("timestampLimit") long timestampLimit);

    long count();

    Optional<SensorReading> findTopBySensorOrderByTimestampDesc(Sensor sensor);
}