package com.bazunia.vps.repository;

import com.bazunia.vps.model.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bazunia.vps.dto.SensorReadingResponseDto;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import com.bazunia.vps.model.User; // <<< DODAJ TEN IMPORT
import org.springframework.data.repository.query.Param;

// <<< DODAJ TE IMPORTY >>>
import org.springframework.data.domain.Pageable;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    // Ta metoda jest wciąż OK
    List<SensorReading> findTop10ByOrderByTimestampDesc();

    // <<< POPRAWKA: ZMIENIAMY TĘ METODĘ >>>
    /**
     * Pobiera ostatnie odczyty, łącząc dane z Sensor (dla 'type').
     * Używa Pageable do ograniczenia wyników (zamiast "LIMIT").
     */
    @Query("SELECT new com.bazunia.vps.dto.SensorReadingResponseDto(" +
            "r.gateway.id, r.sensor.id, r.sensor.type, r.value, r.timestamp, r.id) " +
            "FROM SensorReading r " + // Nie potrzebujemy jawnego JOIN, JPA zrobi to za nas
            "ORDER BY r.timestamp DESC")
    List<SensorReadingResponseDto> findLatestReadingsWithDetails(Pageable pageable); // <<< Zmieniono nazwę i parametr
}