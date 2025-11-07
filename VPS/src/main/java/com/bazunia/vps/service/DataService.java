// 💾 DataService.java
package com.bazunia.vps.service;

import com.bazunia.vps.repository.SensorReadingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataService {

    private final SensorReadingRepository sensorReadingRepository;

    @Autowired
    public DataService(SensorReadingRepository sensorReadingRepository) {
        this.sensorReadingRepository = sensorReadingRepository;
    }

    /**
     * Wymaganie 6.3: Usuwa wszystkie dane czujników z bazy.
     * Używamy @Transactional, aby zapewnić atomowość operacji usuwania.
     */
    @Transactional
    public void deleteAllSensorReadings() {
        // Metoda wbudowana w JpaRepository
        sensorReadingRepository.deleteAll();
        // W realnej aplikacji można by zwrócić informację zwrotną o liczbie usuniętych rekordów
        System.out.println("[DataService] Usunięto całą historię odczytów czujników.");
    }
}