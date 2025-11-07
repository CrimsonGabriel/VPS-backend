// 💾 SensorReadingRepository.java (Poprawione)
package com.bazunia.vps.repository;

import com.bazunia.vps.model.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; // Import potrzebny dla List

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    // --- NOWA METODA (Zamiast findAll()) ---
    // Nazwa metody mówi Spring Data JPA, co ma zrobić:
    // "Znajdź top 10 rekordów, posortowane malejąco po timestamp"
    List<SensorReading> findTop10ByOrderByTimestampDesc();
}