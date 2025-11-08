// 💾 SensorReadingRepository.java (Poprawione)
package com.bazunia.vps.repository;

import com.bazunia.vps.model.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; // Import potrzebny dla List

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {


    List<SensorReading> findTop10ByOrderByTimestampDesc();
}