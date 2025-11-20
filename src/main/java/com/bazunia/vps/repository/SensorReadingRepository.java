package com.bazunia.vps.repository;

import com.bazunia.vps.model.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bazunia.vps.dto.SensorReadingResponseDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    // Metoda dla Androida (wykresy itp.)
    List<SensorReading> findTop10ByOrderByTimestampDesc();

    // Metoda dla DataService (do przycinania tabeli)
    // Zwraca DTO pasujące do konstruktora, który poprawiliśmy wyżej
    @Query("SELECT new com.bazunia.vps.dto.SensorReadingResponseDto(" +
            "r.gateway.id, r.sensor.id, r.sensor.type, r.value, r.timestamp, r.id) " +
            "FROM SensorReading r " +
            "ORDER BY r.timestamp DESC")
    List<SensorReadingResponseDto> findLatestReadingsWithDetails(Pageable pageable);

    /**
     * Usuwa rekordy starsze niż podany timestamp (long).
     * Zmieniono LocalDateTime na long, bo tak masz w encji.
     */
    @Modifying
    @Query("DELETE FROM SensorReading r WHERE r.timestamp < :timestampLimit")
    void deleteByTimestampLessThan(@Param("timestampLimit") long timestampLimit);

    long count();
}